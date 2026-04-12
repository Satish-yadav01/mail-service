package com.satish.mailservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satish.mailservice.aop.TrackTransaction;
import com.satish.mailservice.aop.TransactionIdAspect;
import com.satish.mailservice.dto.EmailHeadersResponse;
import com.satish.mailservice.entity.EmailAuditLog;
import com.satish.mailservice.entity.TransactionStatus;
import com.satish.mailservice.repository.EmailAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/email-info")
@RequiredArgsConstructor
public class EmailInfoController {

    @Value("${email.list.headers}")
    private String emailListHeaders;
    
    private final EmailAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint to get email list headers (Synchronous with Transaction Tracking)
     * GET /api/email-info/emailListHeaders
     */
    @GetMapping("/emailListHeaders")
    @TrackTransaction
    public ResponseEntity<EmailHeadersResponse> getEmailListHeaders() {
        Long tid = null;
        try {
            // Get TID from ThreadLocal
            tid = TransactionIdAspect.getCurrentTid();
            
            // Split the comma-separated headers from properties
            List<String> headers = Arrays.asList(emailListHeaders.split(","));

            EmailHeadersResponse response = new EmailHeadersResponse(
                    true,
                    "Email list headers retrieved successfully",
                    headers
            );

            // Update audit log with success
            updateAuditLog(tid, response, TransactionStatus.SUCCESS, 200, "Headers retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            EmailHeadersResponse errorResponse = new EmailHeadersResponse(
                    false,
                    "Error retrieving email list headers: " + e.getMessage(),
                    null
            );
            
            // Update audit log with failure
            if (tid != null) {
                updateAuditLog(tid, errorResponse, TransactionStatus.FAILED, 500, "Error: " + e.getMessage());
            }
            
            return ResponseEntity.internalServerError().body(errorResponse);
        } finally {
            TransactionIdAspect.clearTid();
        }
    }
    
    private void updateAuditLog(Long tid, EmailHeadersResponse response, TransactionStatus status, int statusCode, String statusMsg) {
        try {
            EmailAuditLog auditLog = auditLogRepository.findById(tid)
                    .orElseThrow(() -> new RuntimeException("Audit log not found for TID: " + tid));
            
            auditLog.setTxnStatus(status);
            auditLog.setStatusCode(statusCode);
            auditLog.setStatusMsg(statusMsg);
            auditLog.setResp(objectMapper.writeValueAsString(response));
            auditLog.setRespTime(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Log error but don't throw - we don't want audit failure to affect the response
            System.err.println("Failed to update audit log for TID: " + tid + ", Error: " + e.getMessage());
        }
    }
}
