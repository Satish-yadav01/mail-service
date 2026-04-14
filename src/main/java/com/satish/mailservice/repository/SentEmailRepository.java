package com.satish.mailservice.repository;

import com.satish.mailservice.entity.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SentEmailRepository extends JpaRepository<SentEmail, Long> {
    List<SentEmail> findByRecipientEmailAndIsColdEmailTrue(String recipientEmail);
    boolean existsByRecipientEmailAndIsColdEmailTrueAndSentAtAfter(String recipientEmail, LocalDateTime sentAt);
}
