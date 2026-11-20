package com.arenahub.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDescIdDesc();

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);

    Optional<AuditLog> findFirstByEntityNameAndEntityIdAndActionOrderByCreatedAtDesc(String entityName, Long entityId, String action);
}
