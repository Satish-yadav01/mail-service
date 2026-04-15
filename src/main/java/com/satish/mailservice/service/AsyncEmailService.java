package com.satish.mailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satish.mailservice.aop.TransactionIdAspect;
import com.satish.mailservice.dto.EmailRequest;
import com.satish.mailservice.dto.EmailResponse;
import com.satish.mailservice.entity.EmailAuditLog;
import com.satish.mailservice.entity.TransactionStatus;
import com.satish.mailservice.repository.EmailAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailService {

    private final EmailService emailService;
    private final EmailAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void processEmailsAsync(Long tid, List<EmailRequest> emailRequests) {
        log.info("Starting async processing for TID: {}", tid);
        
        try {
            // Process emails
            EmailResponse response = emailService.processEmailsSync(emailRequests);
            
            // Update audit log with success
            updateAuditLog(tid, response, TransactionStatus.SUCCESS, 200, "Processing completed successfully");
            
            log.info("Completed async processing for TID: {}", tid);
        } catch (Exception e) {
            log.error("Error in async processing for TID: {}", tid, e);
            
            // Update audit log with failure
            EmailResponse errorResponse = new EmailResponse(false, "Error: " + e.getMessage(), 0, 0, 0);
            updateAuditLog(tid, errorResponse, TransactionStatus.FAILED, 500, "Processing failed: " + e.getMessage());
        }
    }

    @Async
    public void processExcelAsync(Long tid, byte[] fileBytes, String filename) {
        log.info("Starting async Excel processing for TID: {}", tid);
        
        try {
            // Parse and process Excel file from byte array
            EmailResponse response = emailService.importEmailsFromExcel(fileBytes, filename);
            
            // Update audit log with success
            updateAuditLog(tid, response, TransactionStatus.SUCCESS, 200, "Excel import completed successfully");
            
            log.info("Completed async Excel processing for TID: {}", tid);
        } catch (Exception e) {
            log.error("Error in async Excel processing for TID: {}", tid, e);
            
            // Update audit log with failure
            EmailResponse errorResponse = new EmailResponse(false, "Error: " + e.getMessage(), 0, 0, 0);
            updateAuditLog(tid, errorResponse, TransactionStatus.FAILED, 500, "Excel import failed: " + e.getMessage());
        }
    }

    private void updateAuditLog(Long tid, EmailResponse response, TransactionStatus status, int statusCode, String statusMsg) {
        try {
            EmailAuditLog auditLog = auditLogRepository.findById(tid)
                    .orElseThrow(() -> new RuntimeException("Audit log not found for TID: " + tid));
            
            auditLog.setTxnStatus(status);
            auditLog.setStatusCode(statusCode);
            auditLog.setStatusMsg(statusMsg);
            auditLog.setResp(objectMapper.writeValueAsString(response));
            auditLog.setRespTime(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            log.info("Updated audit log for TID: {} with status: {}", tid, status);
        } catch (Exception e) {
            log.error("Failed to update audit log for TID: {}", tid, e);
        }
    }
}
