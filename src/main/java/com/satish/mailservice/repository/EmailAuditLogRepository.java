package com.satish.mailservice.repository;

import com.satish.mailservice.entity.EmailAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailAuditLogRepository extends JpaRepository<EmailAuditLog, Long> {
    Optional<EmailAuditLog> findByTidAndUserId(Long tid, Long userId);

    Page<EmailAuditLog> findByUserId(Long userId, Pageable pageable);
}
