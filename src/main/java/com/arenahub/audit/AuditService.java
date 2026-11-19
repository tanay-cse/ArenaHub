package com.arenahub.audit;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(Long userId, String action, String entityName, Long entityId, String oldValueJson, String newValueJson) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action must not be null or empty.");
        }
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name must not be null or empty.");
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        String ipAddress = resolveClientIpAddress();

        AuditLog auditLog = new AuditLog(
                user,
                action.trim(),
                entityName.trim(),
                entityId,
                oldValueJson,
                newValueJson,
                ipAddress
        );
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    List<AuditLog> getLatestLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDescIdDesc();
    }

    private String resolveClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    String ip = xForwardedFor.split(",")[0].trim();
                    return ip.length() > 45 ? ip.substring(0, 45) : ip;
                }
                String remoteAddr = request.getRemoteAddr();
                if (remoteAddr != null && remoteAddr.length() > 45) {
                    return remoteAddr.substring(0, 45);
                }
                return remoteAddr;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
