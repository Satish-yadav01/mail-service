package com.satish.mailservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satish.mailservice.aop.TrackTransaction;
import com.satish.mailservice.aop.TransactionIdAspect;
import com.satish.mailservice.dto.AsyncResponse;
import com.satish.mailservice.dto.EmailListResponse;
import com.satish.mailservice.dto.EmailRequest;
import com.satish.mailservice.dto.EmailResponse;
import com.satish.mailservice.dto.EnquiryResponse;
import com.satish.mailservice.entity.EmailAuditLog;
import com.satish.mailservice.repository.EmailAuditLogRepository;
import com.satish.mailservice.service.AsyncEmailService;
import com.satish.mailservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final AsyncEmailService asyncEmailService;
    private final EmailAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint 1: Import emails from Excel file (Async)
     * POST /api/emails/import-excel
     */
    @PostMapping("/import-excel")
    @TrackTransaction
    public ResponseEntity<AsyncResponse> importEmailsFromExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AsyncResponse(null, "File is empty", "FAILED"));
            }

            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                        .body(new AsyncResponse(null, "Invalid file format. Please upload an Excel file (.xlsx or .xls)", "FAILED"));
            }

            // Get TID from ThreadLocal
            Long tid = TransactionIdAspect.getCurrentTid();

            // Process asynchronously
            asyncEmailService.processExcelAsync(tid, file);

            // Return immediate acknowledgment
            AsyncResponse response = new AsyncResponse(
                    tid,
                    "Request accepted and processing. Use /api/emails/enquiry?tid=" + tid + " to check status",
                    "PENDING"
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AsyncResponse(null, "Unexpected error: " + e.getMessage(), "FAILED"));
        } finally {
            TransactionIdAspect.clearTid();
        }
    }

    /**
     * Endpoint 2: Add emails directly via POST request (Async)
     * POST /api/emails/add
     */
    @PostMapping("/add")
    @TrackTransaction
    public ResponseEntity<AsyncResponse> addEmailsDirectly(
            @RequestBody List<EmailRequest> emailRequests) {
        try {
            if (emailRequests == null || emailRequests.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AsyncResponse(null, "Email list is empty", "FAILED"));
            }

            // Get TID from ThreadLocal
            Long tid = TransactionIdAspect.getCurrentTid();

            // Process asynchronously
            asyncEmailService.processEmailsAsync(tid, emailRequests);

            // Return immediate acknowledgment
            AsyncResponse response = new AsyncResponse(
                    tid,
                    "Request accepted and processing. Use /api/emails/enquiry?tid=" + tid + " to check status",
                    "PENDING"
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AsyncResponse(null, "Unexpected error: " + e.getMessage(), "FAILED"));
        } finally {
            TransactionIdAspect.clearTid();
        }
    }

    /**
     * Endpoint 3: Enquiry endpoint to check transaction status
     * GET /api/emails/enquiry?tid={tid}
     */
    @GetMapping("/enquiry")
    public ResponseEntity<EnquiryResponse> enquireTransactionStatus(@RequestParam("tid") Long tid) {
        try {
            EmailAuditLog auditLog = auditLogRepository.findById(tid)
                    .orElseThrow(() -> new RuntimeException("Transaction not found with TID: " + tid));

            // Deserialize resp JSON string to EmailResponse object
            EmailResponse emailResponse = null;
            if (auditLog.getResp() != null && !auditLog.getResp().isEmpty()) {
                try {
                    emailResponse = objectMapper.readValue(auditLog.getResp(), EmailResponse.class);
                } catch (Exception e) {
                    // If deserialization fails, leave it as null
                    emailResponse = null;
                }
            }

            EnquiryResponse response = new EnquiryResponse(
                    auditLog.getTid(),
                    auditLog.getMethod(),
                    auditLog.getUrl(),
                    auditLog.getTxnStatus(),
                    auditLog.getStatusCode(),
                    auditLog.getStatusMsg(),
                    emailResponse,
                    auditLog.getReqTime(),
                    auditLog.getRespTime()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new EnquiryResponse(tid, null, null, null, 404, "Transaction not found", null, null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EnquiryResponse(tid, null, null, null, 500, "Error retrieving transaction status: " + e.getMessage(), null, null, null));
        }
    }

    /**
     * Endpoint 4: Get paginated list of emails from database
     * GET /api/emails/list?page={page}&size={size}
     * 
     * @param page Page number (0-indexed, default: 0)
     * @param size Number of records per page (default: 10)
     * @return Paginated list of emails with metadata
     */
    @GetMapping("/list")
    @TrackTransaction
    public ResponseEntity<EmailListResponse> getEmailList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validate parameters
            if (page < 0) {
                return ResponseEntity.badRequest().build();
            }
            
            if (size <= 0 || size > 100) {
                // Limit maximum page size to 100 to prevent performance issues
                size = 10;
            }

            EmailListResponse response = emailService.getEmailList(page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            TransactionIdAspect.clearTid();
        }
    }
}
