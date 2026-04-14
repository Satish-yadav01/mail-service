package com.satish.mailservice.repository;

import com.satish.mailservice.entity.Email;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT e FROM Email e WHERE e.email NOT IN " +
           "(SELECT se.recipientEmail FROM SentEmail se WHERE se.isColdEmail = true) " +
           "ORDER BY e.id ASC")
    List<Email> findUnsentColdEmails(Pageable pageable);
}
