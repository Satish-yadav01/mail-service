package com.satish.mailservice.service;

import com.satish.mailservice.entity.Email;
import com.satish.mailservice.entity.Resume;
import com.satish.mailservice.entity.ResumeStorageType;
import com.satish.mailservice.repository.EmailRepository;
import com.satish.mailservice.repository.ResumeRepository;
import com.satish.mailservice.util.GoogleDriveUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColdEmailSchedulerService {

    private final EmailRepository emailRepository;
    private final ResumeRepository resumeRepository;
    private final MailSenderService mailSenderService;
    private final GoogleDriveUtil googleDriveUtil;

    @Value("${scheduler.cold-email.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${scheduler.cold-email.batch-size:20}")
    private int batchSize;

    @Value("${scheduler.cold-email.interval-minutes:1}")
    private int intervalMinutes;

    /**
     * Scheduled task to send cold emails
     * Runs every day at 8 PM (20:00)
     * Cron expression: 0 0 20 * * ?
     * - Second: 0
     * - Minute: 0
     * - Hour: 20 (8 PM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: ? (any day)
     */
    @Scheduled(cron = "${scheduler.cold-email.cron:0 0 20 * * ?}")
    public void sendScheduledColdEmails() {
        if (!schedulerEnabled) {
            log.info("Cold email scheduler is disabled");
            return;
        }

        log.info("Starting scheduled cold email batch at 8 PM");
        File tempFile = null;

        try {
            // Get active resume
            Optional<Resume> resumeOpt = resumeRepository.findFirstByIsActiveTrueOrderByUploadedAtDesc();
            if (resumeOpt.isEmpty()) {
                log.warn("No active resume found. Skipping cold email batch.");
                return;
            }

            Resume resume = resumeOpt.get();
            File resumeFile;

            // Handle based on storage type
            if (resume.getStorageType() == ResumeStorageType.GOOGLE_DRIVE) {
                log.info("Downloading resume from Google Drive for cold email batch");
                tempFile = googleDriveUtil.downloadFromDrive(resume.getDriveLink(), resume.getFileName());
                resumeFile = tempFile;
            } else {
                // LOCAL storage
                resumeFile = new File(resume.getFilePath());
                if (!resumeFile.exists()) {
                    log.error("Resume file not found: {}", resume.getFilePath());
                    return;
                }
            }

            // Get unsent emails (batch of 20)
            Pageable pageable = PageRequest.of(0, batchSize);
            List<Email> unsentEmails = emailRepository.findUnsentColdEmails(pageable);

            if (unsentEmails.isEmpty()) {
                log.info("No unsent emails found in database");
                return;
            }

            log.info("Found {} unsent emails. Starting to send with {} minute intervals", 
                     unsentEmails.size(), intervalMinutes);

            int successCount = 0;
            int failureCount = 0;

            // Send emails with interval
            for (int i = 0; i < unsentEmails.size(); i++) {
                Email email = unsentEmails.get(i);
                
                try {
                    boolean sent = mailSenderService.sendColdEmail(
                        email.getEmail(),
                        email.getFirstName() != null ? email.getFirstName() + " " + email.getLastName() : null,
                        email.getCompanyName(),
                        resumeFile,
                        resume.getFileName()
                    );

                    if (sent) {
                        successCount++;
                        log.info("Cold email {}/{} sent to: {}", i + 1, unsentEmails.size(), email.getEmail());
                    } else {
                        failureCount++;
                        log.error("Failed to send cold email {}/{} to: {}", i + 1, unsentEmails.size(), email.getEmail());
                    }

                    // Wait for interval before sending next email (except for the last one)
                    if (i < unsentEmails.size() - 1) {
                        log.info("Waiting {} minute(s) before sending next email...", intervalMinutes);
                        TimeUnit.MINUTES.sleep(intervalMinutes);
                    }

                } catch (InterruptedException e) {
                    log.error("Scheduler interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error sending cold email to: {}", email.getEmail(), e);
                }
            }

            log.info("Cold email batch completed. Success: {}, Failed: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("Error in cold email scheduler", e);
        } finally {
            // Clean up temp file if it was downloaded from Drive
            if (tempFile != null) {
                googleDriveUtil.cleanupTempFile(tempFile);
            }
        }
    }

    /**
     * Manual trigger for testing (can be called via API)
     */
    public String triggerManualColdEmailBatch() {
        log.info("Manual cold email batch triggered");
        sendScheduledColdEmails();
        return "Cold email batch triggered successfully";
    }
}
