package com.complymind.user.service;

import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.repository.InstitutionRepository;
import com.complymind.user.entity.User;
import com.complymind.user.entity.UserRole;
import com.complymind.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       InstitutionRepository institutionRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public long getUserCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User registerUser(String username, String email, String rawPassword, String fullName, UserRole role, Long institutionId) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessValidationException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessValidationException("Email '" + email + "' is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);

        if (institutionId != null) {
            Institution institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + institutionId));
            user.setInstitution(institution);
        }

        User saved = userRepository.save(user);
        auditService.recordEvent("USER_CREATED", "User", saved.getId().toString(), "Created user " + username + " with role " + role);
        return saved;
    }

    @Transactional
    public User createInitialAdmin(String username, String email, String rawPassword, String fullName) {
        if (userRepository.count() > 0) {
            throw new BusinessValidationException("Initial administrator already exists");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setFullName(fullName);
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);

        User saved = userRepository.save(admin);
        auditService.recordEvent("INITIAL_SETUP", "User", saved.getId().toString(), "Initial administrator created: " + username);
        return saved;
    }
}
