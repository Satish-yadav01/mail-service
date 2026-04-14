package com.satish.mailservice.service;

import com.satish.mailservice.dto.EmailListResponse;
import com.satish.mailservice.dto.EmailRequest;
import com.satish.mailservice.dto.EmailResponse;
import com.satish.mailservice.entity.Email;
import com.satish.mailservice.entity.EmailStatus;
import com.satish.mailservice.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailRepository emailRepository;

    @Transactional
    public EmailResponse importEmailsFromExcel(MultipartFile file) throws IOException {
        List<EmailRequest> emailRequests = parseExcelFile(file);
        return processEmails(emailRequests);
    }

    @Transactional
    public EmailResponse addEmailsDirectly(List<EmailRequest> emailRequests) {
        return processEmails(emailRequests);
    }

    public EmailResponse processEmailsSync(List<EmailRequest> emailRequests) {
        return processEmails(emailRequests);
    }

    /**
     * Get paginated list of emails from database
     * @param page Page number (0-indexed)
     * @param size Number of records per page (default: 10)
     * @return EmailListResponse with paginated data
     */
    public EmailListResponse getEmailList(int page, int size) {
        // Validate page and size parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10; // Default page size
        }
        
        // Create pageable object with sorting by id descending (latest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        // Fetch paginated data from repository
        Page<Email> emailPage = emailRepository.findAll(pageable);
        
        // Build response
        EmailListResponse response = new EmailListResponse();
        response.setEmails(emailPage.getContent());
        response.setCurrentPage(emailPage.getNumber());
        response.setPageSize(emailPage.getSize());
        response.setTotalElements(emailPage.getTotalElements());
        response.setTotalPages(emailPage.getTotalPages());
        response.setHasNext(emailPage.hasNext());
        response.setHasPrevious(emailPage.hasPrevious());
        
        log.info("Retrieved page {} with {} emails out of {} total", page, emailPage.getNumberOfElements(), emailPage.getTotalElements());
        
        return response;
    }

    private EmailResponse processEmails(List<EmailRequest> emailRequests) {
        int successCount = 0;
        int failureCount = 0;

        for (EmailRequest request : emailRequests) {
            try {
                // Check if email already exists
                if (emailRepository.existsByEmail(request.getEmail())) {
                    log.warn("Email already exists: {}", request.getEmail());
                    failureCount++;
                    continue;
                }

                // Create and save email entity
                Email email = new Email();
                email.setFirstName(request.getFirstName());
                email.setLastName(request.getLastName());
                email.setEmail(request.getEmail());
                email.setEmailStatus(request.getEmailStatus() != null ? request.getEmailStatus() : EmailStatus.UNVERIFIED);
                email.setJobTitle(request.getJobTitle());
                email.setCompanyName(request.getCompanyName());
                email.setCompanyDomain(request.getCompanyDomain());
                email.setLocation(request.getLocation());

                emailRepository.save(email);
                successCount++;
                log.info("Successfully saved email: {}", request.getEmail());
            } catch (Exception e) {
                log.error("Error processing email: {}", request.getEmail(), e);
                failureCount++;
            }
        }

        return new EmailResponse(
                successCount > 0,
                String.format("Processed %d emails: %d successful, %d failed", 
                        emailRequests.size(), successCount, failureCount),
                emailRequests.size(),
                successCount,
                failureCount
        );
    }

    private List<EmailRequest> parseExcelFile(MultipartFile file) throws IOException {
        List<EmailRequest> emailRequests = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Get header row to map column names
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is empty or has no header row");
            }

            // Create a map of column names to indices
            java.util.Map<String, Integer> columnMap = new java.util.HashMap<>();
            for (Cell cell : headerRow) {
                String columnName = cell.getStringCellValue().toLowerCase().trim();
                columnMap.put(columnName, cell.getColumnIndex());
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                EmailRequest emailRequest = new EmailRequest();

                // Map known fields, ignore extra fields
                if (columnMap.containsKey("firstname") || columnMap.containsKey("first name")) {
                    Integer colIndex = columnMap.getOrDefault("firstname", columnMap.get("first name"));
                    if (colIndex != null) {
                        emailRequest.setFirstName(getCellValueAsString(row.getCell(colIndex)));
                    }
                }

                if (columnMap.containsKey("lastname") || columnMap.containsKey("last name")) {
                    Integer colIndex = columnMap.getOrDefault("lastname", columnMap.get("last name"));
                    if (colIndex != null) {
                        emailRequest.setLastName(getCellValueAsString(row.getCell(colIndex)));
                    }
                }

                if (columnMap.containsKey("email")) {
                    emailRequest.setEmail(getCellValueAsString(row.getCell(columnMap.get("email"))));
                }

                if (columnMap.containsKey("emailstatus") || columnMap.containsKey("email status")) {
                    Integer colIndex = columnMap.getOrDefault("emailstatus", columnMap.get("email status"));
                    if (colIndex != null) {
                        String status = getCellValueAsString(row.getCell(colIndex));
                        if (status != null && !status.isEmpty()) {
                            try {
                                emailRequest.setEmailStatus(EmailStatus.valueOf(status.toUpperCase()));
                            } catch (IllegalArgumentException e) {
                                emailRequest.setEmailStatus(EmailStatus.UNVERIFIED);
                            }
                        }
                    }
                }

                if (columnMap.containsKey("jobtitle") || columnMap.containsKey("job title")) {
                    Integer colIndex = columnMap.getOrDefault("jobtitle", columnMap.get("job title"));
                    if (colIndex != null) {
                        emailRequest.setJobTitle(getCellValueAsString(row.getCell(colIndex)));
                    }
                }

                if (columnMap.containsKey("companyname") || columnMap.containsKey("company name")) {
                    Integer colIndex = columnMap.getOrDefault("companyname", columnMap.get("company name"));
                    if (colIndex != null) {
                        emailRequest.setCompanyName(getCellValueAsString(row.getCell(colIndex)));
                    }
                }

                if (columnMap.containsKey("companydomain") || columnMap.containsKey("company domain")) {
                    Integer colIndex = columnMap.getOrDefault("companydomain", columnMap.get("company domain"));
                    if (colIndex != null) {
                        emailRequest.setCompanyDomain(getCellValueAsString(row.getCell(colIndex)));
                    }
                }

                if (columnMap.containsKey("location")) {
                    emailRequest.setLocation(getCellValueAsString(row.getCell(columnMap.get("location"))));
                }

                // Only add if email is present
                if (emailRequest.getEmail() != null && !emailRequest.getEmail().isEmpty()) {
                    emailRequests.add(emailRequest);
                }
            }
        }

        return emailRequests;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}
