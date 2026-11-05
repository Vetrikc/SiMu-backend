package org.example.simu.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.simu.auth.entity.LoginAudit;
import org.example.simu.auth.repository.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records authentication attempts. Runs in its own transaction, so an audit row survives even when
 * the surrounding request ends with an exception (failed login).
 */
@Service
public class LoginAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditService.class);

    private final LoginAuditRepository loginAuditRepository;

    public LoginAuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String username, boolean success, String failureReason,
                       HttpServletRequest request) {
        record(userId, username, success, failureReason, request, "PASSWORD", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String username, boolean success, String failureReason,
                       HttpServletRequest request, String authMethod, String subjectHash) {
        try {
            LoginAudit audit = new LoginAudit();
            audit.setUserId(userId);
            audit.setUsername(trim(username, 255));
            audit.setSuccess(success);
            audit.setFailureReason(trim(failureReason, 100));
            audit.setAuthMethod(trim(authMethod, 20));
            audit.setSubjectHash(trim(subjectHash, 64));
            audit.setIp(trim(clientIp(request), 64));
            audit.setUserAgent(trim(request == null ? null : request.getHeader("User-Agent"), 255));
            loginAuditRepository.save(audit);
        } catch (RuntimeException e) {
            log.warn("Could not write login audit for {}: {}", username, e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}