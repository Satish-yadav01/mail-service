package com.satish.mailservice.controller;

import com.satish.mailservice.dto.ResumeDriveLinkRequest;
import com.satish.mailservice.dto.ResumeUploadResponse;
import com.satish.mailservice.dto.SendMailRequest;
import com.satish.mailservice.dto.SendMailResponse;
import com.satish.mailservice.service.ColdEmailSchedulerService;
import com.satish.mailservice.service.MailSenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {

    private final MailSenderService mailSenderService;
    private final ColdEmailSchedulerService coldEmailSchedulerService;

    /**
     * Upload resume
     * POST /api/mail/resume/upload
     */
    @PostMapping("/resume/upload")
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Resume upload request received: {}", file.getOriginalFilename());
        ResumeUploadResponse response = mailSenderService.uploadResume(file);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Add resume via Google Drive link
     * POST /api/mail/resume/drive-link
     * 
     * Request Body:
     * {
     *   "driveLink": "https://drive.google.com/file/d/FILE_ID/view",
     *   "fileName": "My_Resume.pdf"
     * }
     */
    @PostMapping("/resume/drive-link")
    public ResponseEntity<ResumeUploadResponse> addResumeFromDriveLink(
            @Valid @RequestBody ResumeDriveLinkRequest request) {
        
        log.info("Resume from Google Drive link request received: {}", request.getDriveLink());
        ResumeUploadResponse response = mailSenderService.addResumeFromDriveLink(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Send email with resume attachment
     * POST /api/mail/send
     * 
     * Request Body:
     * {
     *   "to": "recipient@example.com",
     *   "subject": "Application for Software Developer Position",
     *   "message": "Dear Hiring Manager, I am writing to express my interest..."
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<SendMailResponse> sendEmail(
            @Valid @RequestBody SendMailRequest request) {
        
        log.info("Send email request received for: {}", request.getTo());
        SendMailResponse response = mailSenderService.sendEmailWithResume(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Manually trigger cold email batch (for testing)
     * POST /api/mail/cold-email/trigger
     */
    @PostMapping("/cold-email/trigger")
    public ResponseEntity<String> triggerColdEmailBatch() {
        log.info("Manual cold email batch trigger request received");
        String result = coldEmailSchedulerService.triggerManualColdEmailBatch();
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     * GET /api/mail/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mail service is running");
    }
}
