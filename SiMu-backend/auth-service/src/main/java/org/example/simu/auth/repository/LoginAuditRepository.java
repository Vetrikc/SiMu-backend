package org.example.simu.auth.repository;

import org.example.simu.auth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {

    List<LoginAudit> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
