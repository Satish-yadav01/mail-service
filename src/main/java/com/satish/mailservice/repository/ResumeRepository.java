package com.satish.mailservice.repository;

import com.satish.mailservice.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findFirstByIsActiveTrueOrderByUploadedAtDesc();
}
