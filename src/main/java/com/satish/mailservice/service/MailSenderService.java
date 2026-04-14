package com.satish.mailservice.service;

import com.satish.mailservice.dto.ResumeDriveLinkRequest;
import com.satish.mailservice.dto.ResumeUploadResponse;
import com.satish.mailservice.dto.SendMailRequest;
import com.satish.mailservice.dto.SendMailResponse;
import com.satish.mailservice.entity.Resume;
import com.satish.mailservice.entity.ResumeStorageType;
import com.satish.mailservice.entity.SentEmail;
import com.satish.mailservice.repository.ResumeRepository;
import com.satish.mailservice.repository.SentEmailRepository;
import com.satish.mailservice.util.GoogleDriveUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailSenderService {

    private final JavaMailSender mailSender;
    private final ResumeRepository resumeRepository;
    private final SentEmailRepository sentEmailRepository;
    private final GoogleDriveUtil googleDriveUtil;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${resume.storage.path}")
    private String resumeStoragePath;

    /**
     * Add resume via Google Drive link
     */
    @Transactional
    public ResumeUploadResponse addResumeFromDriveLink(ResumeDriveLinkRequest request) {
        try {
            // Validate Drive link
            if (!googleDriveUtil.isValidDriveLink(request.getDriveLink())) {
                return new ResumeUploadResponse(false, "Invalid Google Drive link format", null, null);
            }

            // Extract file ID to validate
            String fileId = googleDriveUtil.extractFileId(request.getDriveLink());
            log.info("Extracted Google Drive file ID: {}", fileId);

            // Deactivate previous resumes
            resumeRepository.findAll().forEach(resume -> {
                resume.setIsActive(false);
                resumeRepository.save(resume);
            });

            // Determine filename
            String fileName = request.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "Resume_" + fileId + ".pdf";
            }

            // Save resume metadata with Drive link
            Resume resume = new Resume();
            resume.setFileName(fileName);
            resume.setFilePath(request.getDriveLink()); // Store drive link as path for backward compatibility
            resume.setDriveLink(request.getDriveLink());
            resume.setStorageType(ResumeStorageType.GOOGLE_DRIVE);
            resume.setContentType("application/pdf"); // Default to PDF
            resume.setFileSize(0L); // Size unknown until download
            resume.setIsActive(true);
            resume = resumeRepository.save(resume);

            log.info("Resume from Google Drive added successfully: {}", fileName);
            return new ResumeUploadResponse(true, "Resume from Google Drive added successfully", resume.getId(), fileName);

        } catch (Exception e) {
            log.error("Error adding resume from Google Drive", e);
            return new ResumeUploadResponse(false, "Error adding resume from Google Drive: " + e.getMessage(), null, null);
        }
    }

    /**
     * Upload and store resume file
     */
    @Transactional
    public ResumeUploadResponse uploadResume(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return new ResumeUploadResponse(false, "File is empty", null, null);
            }

            // Validate file type (PDF, DOC, DOCX)
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") 
                    && !contentType.equals("application/msword")
                    && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                return new ResumeUploadResponse(false, "Only PDF, DOC, and DOCX files are allowed", null, null);
            }

            // Create storage directory if not exists
            Path storagePath = Paths.get(resumeStoragePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = storagePath.resolve(uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Deactivate previous resumes
            resumeRepository.findAll().forEach(resume -> {
                resume.setIsActive(false);
                resumeRepository.save(resume);
            });

            // Save resume metadata
            Resume resume = new Resume();
            resume.setFileName(originalFilename);
            resume.setFilePath(filePath.toString());
            resume.setContentType(contentType);
            resume.setFileSize(file.getSize());
            resume.setIsActive(true);
            resume = resumeRepository.save(resume);

            log.info("Resume uploaded successfully: {}", originalFilename);
            return new ResumeUploadResponse(true, "Resume uploaded successfully", resume.getId(), originalFilename);

        } catch (IOException e) {
            log.error("Error uploading resume", e);
            return new ResumeUploadResponse(false, "Error uploading resume: " + e.getMessage(), null, null);
        }
    }

    /**
     * Send email with resume attachment
     */
    @Transactional
    public SendMailResponse sendEmailWithResume(SendMailRequest request) {
        File tempFile = null;
        try {
            // Get active resume
            Optional<Resume> resumeOpt = resumeRepository.findFirstByIsActiveTrueOrderByUploadedAtDesc();
            if (resumeOpt.isEmpty()) {
                return new SendMailResponse(false, "No active resume found. Please upload a resume first.", request.getTo());
            }

            Resume resume = resumeOpt.get();
            File resumeFile;

            // Handle based on storage type
            if (resume.getStorageType() == ResumeStorageType.GOOGLE_DRIVE) {
                log.info("Downloading resume from Google Drive for email to: {}", request.getTo());
                tempFile = googleDriveUtil.downloadFromDrive(resume.getDriveLink(), resume.getFileName());
                resumeFile = tempFile;
            } else {
                // LOCAL storage
                resumeFile = new File(resume.getFilePath());
                if (!resumeFile.exists()) {
                    return new SendMailResponse(false, "Resume file not found on server", request.getTo());
                }
            }

            // Send email
            sendEmail(request.getTo(), request.getSubject(), request.getMessage(), resumeFile, resume.getFileName(), false);

            // Log sent email
            saveSentEmail(request.getTo(), null, request.getSubject(), request.getMessage(), false, true, "SUCCESS", null);

            log.info("Email sent successfully to: {}", request.getTo());
            return new SendMailResponse(true, "Email sent successfully with resume attachment", request.getTo());

        } catch (Exception e) {
            log.error("Error sending email to: {}", request.getTo(), e);
            saveSentEmail(request.getTo(), null, request.getSubject(), request.getMessage(), false, true, "FAILED", e.getMessage());
            return new SendMailResponse(false, "Error sending email: " + e.getMessage(), request.getTo());
        } finally {
            // Clean up temp file if it was downloaded from Drive
            if (tempFile != null) {
                googleDriveUtil.cleanupTempFile(tempFile);
            }
        }
    }

    /**
     * Send cold email with resume (used by scheduler)
     */
    @Transactional
    public boolean sendColdEmail(String recipientEmail, String recipientName, String companyName, File resumeFile, String resumeFileName) {
        try {
            String subject = "Exploring Opportunities at " + (companyName != null ? companyName : "Your Organization");
            String message = buildColdEmailMessage(recipientName, companyName);

            sendEmail(recipientEmail, subject, message, resumeFile, resumeFileName, false);

            // Log sent email
            saveSentEmail(recipientEmail, recipientName, subject, message, true, true, "SUCCESS", null);

            log.info("Cold email sent successfully to: {}", recipientEmail);
            return true;

        } catch (Exception e) {
            log.error("Error sending cold email to: {}", recipientEmail, e);
            String subject = "Exploring Opportunities at " + (companyName != null ? companyName : "Your Organization");
            String message = buildColdEmailMessage(recipientName, companyName);
            saveSentEmail(recipientEmail, recipientName, subject, message, true, true, "FAILED", e.getMessage());
            return false;
        }
    }

    /**
     * Build cold email message
     */
    private String buildColdEmailMessage(String recipientName, String companyName) {
        StringBuilder message = new StringBuilder();
        
        message.append("Dear ").append(recipientName != null ? recipientName : "Hiring Manager").append(",\n\n");
        
        message.append("I hope this email finds you well. My name is Satish Yadav, and I am reaching out to express my interest in exploring potential opportunities at ");
        message.append(companyName != null ? companyName : "your esteemed organization").append(".\n\n");
        
        message.append("I am a passionate software developer with expertise in Java, Spring Boot, and modern web technologies. ");
        message.append("I have a strong background in building scalable applications and solving complex technical challenges.\n\n");
        
        message.append("I have attached my resume for your review. I would be grateful for the opportunity to discuss how my skills and experience ");
        message.append("align with your team's needs and contribute to ").append(companyName != null ? companyName + "'s" : "your organization's").append(" success.\n\n");
        
        message.append("Thank you for considering my application. I look forward to the possibility of connecting with you.\n\n");
        
        message.append("Best regards,\n");
        message.append("Satish Yadav\n");
        message.append("Email: yadavsatish.ssy02@gmail.com");
        
        return message.toString();
    }

    /**
     * Send email using JavaMailSender
     */
    private void sendEmail(String to, String subject, String message, File attachment, String attachmentName, boolean isHtml) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(message, isHtml);

        if (attachment != null && attachment.exists()) {
            FileSystemResource file = new FileSystemResource(attachment);
            // Use provided attachment name or fall back to file name
            String fileName = (attachmentName != null && !attachmentName.isEmpty()) ? attachmentName : attachment.getName();
            helper.addAttachment(fileName, file);
        }

        mailSender.send(mimeMessage);
    }

    /**
     * Save sent email record
     */
    private void saveSentEmail(String recipientEmail, String recipientName, String subject, 
                               String message, boolean isColdEmail, boolean hasAttachment, 
                               String status, String errorMessage) {
        try {
            SentEmail sentEmail = new SentEmail();
            sentEmail.setRecipientEmail(recipientEmail);
            sentEmail.setRecipientName(recipientName);
            sentEmail.setSubject(subject);
            sentEmail.setMessage(message);
            sentEmail.setIsColdEmail(isColdEmail);
            sentEmail.setHasAttachment(hasAttachment);
            sentEmail.setStatus(status);
            sentEmail.setErrorMessage(errorMessage);
            sentEmailRepository.save(sentEmail);
        } catch (Exception e) {
            log.error("Error saving sent email record", e);
        }
    }
}
