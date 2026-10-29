package com.complymind.audit.service;

import com.complymind.audit.entity.AuditEvent;
import com.complymind.audit.repository.AuditEventRepository;
import com.complymind.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent recordEvent(String action, String entityType, String entityId, String details) {
        String username = SecurityUtils.getCurrentUsername();
        String role = SecurityUtils.getCurrentRoleName();
        AuditEvent event = new AuditEvent(username, role, action, entityType, entityId, details, "127.0.0.1");
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getAuditEvents(Pageable pageable) {
        return auditEventRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getRecentEvents() {
        return auditEventRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsForEntity(String entityType, String entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }
}
