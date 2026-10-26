# Async Excel Processing Fix

## Problem

The application was experiencing a `java.nio.file.NoSuchFileException` when processing Excel files asynchronously. The error occurred at:

```
at com.satish.mailservice.service.EmailService.parseExcelFile(EmailService.java:129)
at com.satish.mailservice.service.EmailService.importEmailsFromExcel(EmailService.java:34)
```

### Root Cause

When a `MultipartFile` is uploaded to a Spring Boot controller:
1. Tomcat creates a temporary file to store the uploaded content
2. The controller method receives a reference to this temporary file
3. When the controller method returns, Tomcat automatically deletes the temporary file
4. If the file is passed to an `@Async` method, the async method executes in a separate thread
5. By the time the async method tries to read the file, it has already been deleted by Tomcat

This is a classic race condition with async processing of multipart files.

## Solution

The fix involves reading the file content **before** passing it to the async method:

### Changes Made

#### 1. EmailController.java
- Read the file bytes in the controller (before async processing)
- Pass the byte array and filename to the async method instead of the MultipartFile

```java
// Read file bytes before async processing to avoid temp file deletion
byte[] fileBytes = file.getBytes();
String originalFilename = file.getOriginalFilename();

// Process asynchronously
asyncEmailService.processExcelAsync(tid, fileBytes, originalFilename);
```

#### 2. AsyncEmailService.java
- Changed method signature from `processExcelAsync(Long tid, MultipartFile file)` to `processExcelAsync(Long tid, byte[] fileBytes, String filename)`
- Now accepts byte array instead of MultipartFile

```java
@Async
public void processExcelAsync(Long tid, byte[] fileBytes, String filename) {
    // Parse and process Excel file from byte array
    EmailResponse response = emailService.importEmailsFromExcel(fileBytes, filename);
    // ...
}
```

#### 3. EmailService.java
- Added new overloaded method: `importEmailsFromExcel(byte[] fileBytes, String filename)`
- Added new private method: `parseExcelFile(byte[] fileBytes, String filename)`
- Refactored existing `parseExcelFile(MultipartFile file)` to use a common `parseExcelFile(InputStream inputStream)` method
- All parsing methods now delegate to the InputStream-based implementation

```java
@Transactional
public EmailResponse importEmailsFromExcel(byte[] fileBytes, String filename) throws IOException {
    List<EmailRequest> emailRequests = parseExcelFile(fileBytes, filename);
    return processEmails(emailRequests);
}

private List<EmailRequest> parseExcelFile(byte[] fileBytes, String filename) throws IOException {
    return parseExcelFile(new ByteArrayInputStream(fileBytes));
}

private List<EmailRequest> parseExcelFile(InputStream inputStream) throws IOException {
    // Common implementation for parsing Excel from InputStream
    // ...
}
```

## Benefits

1. **Eliminates Race Condition**: File content is captured before the temporary file is deleted
2. **Thread-Safe**: Byte arrays are immutable and can be safely passed between threads
3. **Backward Compatible**: The synchronous file upload still works as before
4. **Clean Architecture**: Separation of concerns - controller handles file I/O, service handles business logic

## Testing

To verify the fix:

1. Upload an Excel file using the `/api/emails/import-excel` endpoint
2. Check the transaction status using `/api/emails/enquiry?tid={tid}`
3. Verify that the status shows SUCCESS and emails are imported correctly

Example:
```bash
# Upload Excel file
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@emails.xlsx"

# Check status (replace {tid} with actual transaction ID)
curl http://localhost:8080/api/emails/enquiry?tid={tid}
```

## Additional Notes

- The fix does not impact memory significantly as the file bytes are only held temporarily
- For very large files (>100MB), consider implementing streaming or chunked processing
- The original MultipartFile-based method is still available for synchronous processing if needed
