# Audit Log Enhancement - Method, URL, and Request Body

## Changes Made

### 1. Added New Columns to EmailAuditLog Entity

**New Fields:**
- `method` - HTTP method (GET, POST, PUT, DELETE, etc.)
- `url` - Request URI/endpoint

**Updated Entity:**
```java
@Entity
@Table(name = "email_audit_req_resp_log")
public class EmailAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tid;
    
    private String method;      // NEW: HTTP method
    private String url;          // NEW: Request URL
    
    @Column(columnDefinition = "TEXT")
    private String req;          // UPDATED: Now stores actual request body
    
    @Column(columnDefinition = "TEXT")
    private String resp;
    
    // ... other fields
}
```

### 2. Updated Request Capture Logic

**Before:**
```
req: "Method: POST, URI: /api/emails/add, Args: addEmailsDirectly"
```

**After:**
```
method: "POST"
url: "/api/emails/add"
req: "[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\"}]"
```

### 3. Request Body Capture

The AOP aspect now captures:
- **For JSON requests**: The actual request body serialized as JSON
- **For file uploads**: File metadata (filename, size, content type)

**Example for /add endpoint:**
```json
{
    "method": "POST",
    "url": "/api/emails/add",
    "req": "[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\",\"emailStatus\":\"VERIFIED\"}]"
}
```

**Example for /import-excel endpoint:**
```json
{
    "method": "POST",
    "url": "/api/emails/import-excel",
    "req": "{\"fileName\":\"emails.xlsx\",\"fileSize\":15360,\"contentType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"}"
}
```

## Database Schema Update

The `email_audit_req_resp_log` table now has:

```sql
CREATE TABLE email_audit_req_resp_log (
    tid BIGINT AUTO_INCREMENT PRIMARY KEY,
    method VARCHAR(10),                    -- NEW
    url VARCHAR(255),                      -- NEW
    req TEXT,                              -- UPDATED: stores request body
    resp TEXT,
    req_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resp_time TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status_code INT,
    status_msg VARCHAR(255),
    txn_status VARCHAR(20)
);
```

## Updated API Response

### Enquiry Endpoint Response

**Before:**
```json
{
    "tid": 1,
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Processing completed successfully",
    "resp": {...},
    "reqTime": "2026-04-12 18:09:49",
    "respTime": "2026-04-12 18:09:51"
}
```

**After:**
```json
{
    "tid": 1,
    "method": "POST",
    "url": "/api/emails/add",
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Processing completed successfully",
    "resp": {
        "success": true,
        "message": "Processed 2 emails: 2 successful, 0 failed",
        "totalProcessed": 2,
        "successCount": 2,
        "failureCount": 0
    },
    "reqTime": "2026-04-12 18:09:49",
    "respTime": "2026-04-12 18:09:51"
}
```

## Database Record Example

**Sample record in database:**
```json
{
    "tid": 1,
    "method": "POST",
    "url": "/api/emails/add",
    "req": "[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\",\"emailStatus\":\"VERIFIED\",\"jobTitle\":\"Developer\",\"companyName\":\"Tech Corp\",\"companyDomain\":\"techcorp.com\",\"location\":\"San Francisco\"}]",
    "resp": "{\"success\":true,\"message\":\"Processed 1 emails: 1 successful, 0 failed\",\"totalProcessed\":1,\"successCount\":1,\"failureCount\":0}",
    "req_time": "2026-04-12 18:20:00",
    "resp_time": "2026-04-12 18:20:02",
    "status_code": 200,
    "status_msg": "Processing completed successfully",
    "txn_status": "SUCCESS",
    "updated_at": "2026-04-12 18:20:02"
}
```

## Benefits

### 1. Better Debugging
- Can see exactly what was sent in the request
- Easy to reproduce issues by replaying the request
- Clear separation of HTTP metadata (method, URL) and payload (req)

### 2. Audit Trail
- Complete record of what data was submitted
- Can track which endpoints are being used
- Can analyze request patterns

### 3. Troubleshooting
- If a request fails, you can see the exact input
- Can validate if the issue was with the request data
- Can test with the same data to reproduce issues

### 4. Analytics
- Track which HTTP methods are most used
- Identify most frequently called endpoints
- Analyze request sizes and patterns

## Implementation Details

### AOP Aspect Logic

```java
private String captureRequestBody(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    
    // For JSON requests (List<EmailRequest>)
    // Serializes to: "[{...}, {...}]"
    
    // For file uploads (MultipartFile)
    // Returns: {"fileName":"...", "fileSize":..., "contentType":"..."}
}
```

### Request Body Handling

1. **JSON Requests**: Serialized using ObjectMapper
2. **File Uploads**: Metadata extracted (filename, size, type)
3. **Empty/Null**: Returns "Request body unavailable"

## Files Modified

1. **EmailAuditLog.java** - Added `method` and `url` fields
2. **TransactionIdAspect.java** - Updated to capture method, URL, and actual request body
3. **EnquiryResponse.java** - Added `method` and `url` fields
4. **EmailController.java** - Updated enquiry response to include new fields

## Testing

### Test 1: Add Emails
```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "emailStatus": "VERIFIED"
    }
  ]'
```

**Response:**
```json
{
    "tid": 1,
    "message": "Request accepted and processing. Use /api/emails/enquiry?tid=1 to check status",
    "status": "PENDING"
}
```

### Test 2: Check Status
```bash
curl -X GET "http://localhost:8080/api/emails/enquiry?tid=1"
```

**Response:**
```json
{
    "tid": 1,
    "method": "POST",
    "url": "/api/emails/add",
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Processing completed successfully",
    "resp": {
        "success": true,
        "message": "Processed 1 emails: 1 successful, 0 failed",
        "totalProcessed": 1,
        "successCount": 1,
        "failureCount": 0
    },
    "reqTime": "2026-04-12 18:25:30",
    "respTime": "2026-04-12 18:25:32"
}
```

### Test 3: Import Excel
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@emails.xlsx"
```

**Database Record:**
```json
{
    "method": "POST",
    "url": "/api/emails/import-excel",
    "req": "{\"fileName\":\"emails.xlsx\",\"fileSize\":15360,\"contentType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"}"
}
```

## Migration Notes

⚠️ **Database Migration Required**

If you have existing data:
1. Backup your database
2. Add the new columns:
```sql
ALTER TABLE email_audit_req_resp_log 
ADD COLUMN method VARCHAR(10) AFTER tid,
ADD COLUMN url VARCHAR(255) AFTER method;
```

Or drop and recreate (if no important data):
```sql
DROP TABLE email_audit_req_resp_log;
-- Restart application to recreate with new schema
```

## Compilation Status

✅ **BUILD SUCCESS** - All changes compiled successfully

## Next Steps

1. Restart the application
2. The table will be updated with new columns
3. Test the endpoints to verify request body capture
4. Check the database to see the new structure
5. Use the enquiry endpoint to see the enhanced response
