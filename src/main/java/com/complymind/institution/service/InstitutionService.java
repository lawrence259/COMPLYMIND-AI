package com.complymind.institution.service;

import com.complymind.audit.service.AuditService;
import com.complymind.common.exception.BusinessValidationException;
import com.complymind.common.exception.ResourceNotFoundException;
import com.complymind.institution.entity.Institution;
import com.complymind.institution.repository.InstitutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final AuditService auditService;

    public InstitutionService(InstitutionRepository institutionRepository, AuditService auditService) {
        this.institutionRepository = institutionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Institution> getAllInstitutions() {
        return institutionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Institution> getInstitutions(Pageable pageable) {
        return institutionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Institution getInstitutionById(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + id));
    }

    @Transactional
    public Institution createInstitution(Institution institution) {
        if (institutionRepository.existsByCode(institution.getCode())) {
            throw new BusinessValidationException("Institution code '" + institution.getCode() + "' already exists");
        }
        Institution saved = institutionRepository.save(institution);
        auditService.recordEvent("INSTITUTION_REGISTERED", "Institution", saved.getId().toString(),
                "Registered institution: " + saved.getName() + " (" + saved.getCode() + ")");
        return saved;
    }

    @Transactional
    public Institution updateInstitution(Long id, Institution updated) {
        Institution existing = getInstitutionById(id);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setAictePermanentId(updated.getAictePermanentId());
        existing.setState(updated.getState());
        existing.setCity(updated.getCity());
        existing.setAddress(updated.getAddress());
        existing.setContactEmail(updated.getContactEmail());
        existing.setContactPhone(updated.getContactPhone());

        Institution saved = institutionRepository.save(existing);
        auditService.recordEvent("INSTITUTION_UPDATED", "Institution", saved.getId().toString(),
                "Updated details for: " + saved.getName());
        return saved;
    }
}
