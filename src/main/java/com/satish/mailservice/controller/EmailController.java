package com.satish.mailservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satish.mailservice.aop.TrackTransaction;
import com.satish.mailservice.aop.TransactionIdAspect;
import com.satish.mailservice.dto.AsyncResponse;
import com.satish.mailservice.dto.EmailAuditLogListResponse;
import com.satish.mailservice.dto.EmailAuditLogSummary;
import com.satish.mailservice.dto.EmailListResponse;
import com.satish.mailservice.dto.EmailRequest;
import com.satish.mailservice.dto.EmailResponse;
import com.satish.mailservice.dto.EnquiryResponse;
import com.satish.mailservice.entity.EmailAuditLog;
import com.satish.mailservice.exception.UserAccessException;
import com.satish.mailservice.repository.EmailAuditLogRepository;
import com.satish.mailservice.service.AsyncEmailService;
import com.satish.mailservice.service.EmailService;
import com.satish.mailservice.service.UserAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;
    private final AsyncEmailService asyncEmailService;
    private final EmailAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final UserAccessService userAccessService;

    /**
     * Endpoint 1: Import emails from Excel file (Async)
     * POST /api/emails/import-excel
     */
    @PostMapping("/import-excel")
    @TrackTransaction
    public ResponseEntity<AsyncResponse> importEmailsFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("user_id") Long userId) {
        try {
            log.info("Importing emails from excel with TID: {} and user_id : {}", TransactionIdAspect.getCurrentTid(), userId);
            userAccessService.validateEnabledUser(userId);

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

            // Read file bytes before async processing to avoid temp file deletion
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // Process asynchronously
            asyncEmailService.processExcelAsync(tid, fileBytes, originalFilename);

            // Return immediate acknowledgment
            AsyncResponse response = new AsyncResponse(
                    tid,
                    "Request accepted and processing. Use /api/emails/enquiry?tid=" + tid + " to check status",
                    "PENDING"
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (UserAccessException e) {
            log.error("UserAccessException occurred while validating user access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AsyncResponse(null, "Unexpected error: " + e.getMessage(), "FAILED"));
        } catch (Exception e) {
            log.error("Error occurred while validating user access", e);
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
            @RequestBody List<EmailRequest> emailRequests,
            @RequestParam("user_id") Long userId) {
        try {
            log.info("Adding emails directly with TID: {} and user_id : {}", TransactionIdAspect.getCurrentTid(), userId);
            userAccessService.validateEnabledUser(userId);

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
        } catch (UserAccessException e) {
            log.error("UserAccessException occurred while adding emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AsyncResponse(null, "Unexpected error: " + e.getMessage(), "FAILED"));
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
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
    public ResponseEntity<EnquiryResponse> enquireTransactionStatus(
            @RequestParam("tid") Long tid,
            @RequestParam("user_id") Long userId) {
        try {
            log.info("Enquiry for transaction status with TID: {} and user_id : {}", tid, userId);
            userAccessService.validateEnabledUser(userId);
            EmailAuditLog auditLog = auditLogRepository.findByTidAndUserId(tid, userId)
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
        } catch (UserAccessException e) {
            log.error("UserAccessException occurred while validating user access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EnquiryResponse(tid, null, null, null, 500, "Unexpected error: " + e.getMessage(), null, null, null));
        } catch (RuntimeException e) {
            log.error("RuntimeException occurred", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new EnquiryResponse(tid, null, null, null, 404, "Transaction not found", null, null, null));
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
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
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam("user_id") Long userId) {
        try {
            log.info("Fetching email list with TID: {} and user_id : {}", TransactionIdAspect.getCurrentTid(), userId);
            userAccessService.validateEnabledUser(userId);

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
        } catch (UserAccessException e) {
            log.error("Error occurred while validating user access", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            TransactionIdAspect.clearTid();
        }
    }

    /**
     * Get paginated audit logs for a user.
     * GET /api/emails/transactions?user_id={userId}&page={page}&size={size}
     */
    @GetMapping("/transactions")
    public ResponseEntity<EmailAuditLogListResponse> getUserAuditLogs(
            @RequestParam("user_id") Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        userAccessService.validateEnabledUser(userId);

        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }

        size = Math.min(size, 100);
        Page<EmailAuditLog> auditLogPage = auditLogRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tid"))
        );

        List<EmailAuditLogSummary> transactions = auditLogPage.getContent().stream()
                .map(auditLog -> new EmailAuditLogSummary(
                        auditLog.getTid(),
                        auditLog.getUserId(),
                        auditLog.getUrl(),
                        auditLog.getStatusCode(),
                        auditLog.getStatusMsg(),
                        auditLog.getTxnStatus()
                ))
                .toList();

        EmailAuditLogListResponse response = new EmailAuditLogListResponse(
                transactions,
                auditLogPage.getNumber(),
                auditLogPage.getSize(),
                auditLogPage.getTotalElements(),
                auditLogPage.getTotalPages(),
                auditLogPage.hasNext(),
                auditLogPage.hasPrevious()
        );
        return ResponseEntity.ok(response);
    }


}
