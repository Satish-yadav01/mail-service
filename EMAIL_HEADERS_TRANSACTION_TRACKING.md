# Email List Headers Endpoint - Transaction Tracking

## Overview
The `/api/email-info/emailListHeaders` endpoint now includes transaction tracking with TID generation and audit logging, but remains **synchronous** (not async).

## Implementation

### Transaction Tracking Flow

1. **Request arrives** → `@TrackTransaction` annotation triggers AOP
2. **TID generated** → Auto-increment ID created in database
3. **Audit log created** → Initial record with PENDING status
4. **Endpoint executes** → Headers retrieved synchronously
5. **Audit log updated** → Final status (SUCCESS/FAILED) with response
6. **Response returned** → Client receives headers immediately

## Key Differences from Async Endpoints

| Feature | Async Endpoints (/add, /import-excel) | Sync Endpoint (/emailListHeaders) |
|---------|---------------------------------------|-----------------------------------|
| **Response Time** | Immediate (202 Accepted) | After processing (200 OK) |
| **TID in Response** | Yes (for polling) | No (not needed) |
| **Processing** | Background thread | Same request thread |
| **Status** | PENDING → SUCCESS/FAILED | Directly SUCCESS/FAILED |
| **Use Case** | Long-running operations | Quick operations |

## API Behavior

### Request
```bash
GET /api/email-info/emailListHeaders
```

### Response (200 OK)
```json
{
    "success": true,
    "message": "Email list headers retrieved successfully",
    "headers": [
        "SNo",
        "Name",
        "Email",
        "Job title",
        "Company name",
        "First name",
        "Last name"
    ]
}
```

**Note:** The TID is NOT returned in the response because this is a synchronous operation. The client doesn't need to poll for status.

## Database Audit Log

### Record Created

When you call the endpoint, a record is created in `email_audit_req_resp_log`:

```json
{
    "tid": 5,
    "method": "GET",
    "url": "/api/email-info/emailListHeaders",
    "req": "Request body unavailable",
    "resp": "{\"success\":true,\"message\":\"Email list headers retrieved successfully\",\"headers\":[\"SNo\",\"Name\",\"Email\",\"Job title\",\"Company name\",\"First name\",\"Last name\"]}",
    "req_time": "2026-04-12 18:45:00",
    "resp_time": "2026-04-12 18:45:00",
    "status_code": 200,
    "status_msg": "Headers retrieved successfully",
    "txn_status": "SUCCESS",
    "updated_at": "2026-04-12 18:45:00"
}
```

### Key Points

1. **req_time = resp_time**: Because it's synchronous, both timestamps are nearly identical
2. **txn_status**: Directly set to SUCCESS or FAILED (no PENDING state)
3. **req**: "Request body unavailable" (GET request has no body)
4. **resp**: Full response JSON stored

## Code Implementation

### Controller Method

```java
@GetMapping("/emailListHeaders")
@TrackTransaction  // ← Enables transaction tracking
public ResponseEntity<EmailHeadersResponse> getEmailListHeaders() {
    Long tid = null;
    try {
        // Get TID from ThreadLocal (set by AOP)
        tid = TransactionIdAspect.getCurrentTid();
        
        // Process request
        List<String> headers = Arrays.asList(emailListHeaders.split(","));
        EmailHeadersResponse response = new EmailHeadersResponse(
            true,
            "Email list headers retrieved successfully",
            headers
        );

        // Update audit log with success
        updateAuditLog(tid, response, TransactionStatus.SUCCESS, 200, 
                      "Headers retrieved successfully");

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        // Update audit log with failure
        if (tid != null) {
            updateAuditLog(tid, errorResponse, TransactionStatus.FAILED, 500, 
                          "Error: " + e.getMessage());
        }
        return ResponseEntity.internalServerError().body(errorResponse);
    } finally {
        TransactionIdAspect.clearTid();  // Clean up ThreadLocal
    }
}
```

## Benefits of Synchronous Tracking

### 1. **Audit Trail**
- Every request is logged
- Can track who accessed headers and when
- Useful for compliance and debugging

### 2. **Performance Monitoring**
- Track response times
- Identify slow requests
- Monitor success/failure rates

### 3. **Debugging**
- See exact request and response
- Trace issues with specific TIDs
- Analyze error patterns

### 4. **No Overhead**
- Client doesn't need to poll
- Simpler client implementation
- Immediate response

## Querying Audit Logs

You can query the audit logs for this endpoint:

### Get All Header Requests
```sql
SELECT * FROM email_audit_req_resp_log 
WHERE url = '/api/email-info/emailListHeaders'
ORDER BY req_time DESC;
```

### Get Failed Requests
```sql
SELECT * FROM email_audit_req_resp_log 
WHERE url = '/api/email-info/emailListHeaders' 
AND txn_status = 'FAILED';
```

### Get Recent Requests
```sql
SELECT tid, method, url, status_code, txn_status, req_time 
FROM email_audit_req_resp_log 
WHERE url = '/api/email-info/emailListHeaders'
ORDER BY req_time DESC 
LIMIT 10;
```

## Using the Enquiry Endpoint

Even though this is synchronous, you can still use the enquiry endpoint to check the audit log:

```bash
curl -X GET "http://localhost:8080/api/emails/enquiry?tid=5"
```

**Response:**
```json
{
    "tid": 5,
    "method": "GET",
    "url": "/api/email-info/emailListHeaders",
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Headers retrieved successfully",
    "resp": {
        "success": true,
        "message": "Email list headers retrieved successfully",
        "headers": [
            "SNo",
            "Name",
            "Email",
            "Job title",
            "Company name",
            "First name",
            "Last name"
        ]
    },
    "reqTime": "2026-04-12 18:45:00",
    "respTime": "2026-04-12 18:45:00"
}
```

## Testing

### Test 1: Normal Request
```bash
curl -X GET http://localhost:8080/api/email-info/emailListHeaders
```

**Expected:**
- ✅ 200 OK response
- ✅ Headers returned
- ✅ Audit log created with SUCCESS status

### Test 2: Check Audit Log
```bash
# Get the TID from database
curl -X GET "http://localhost:8080/api/emails/enquiry?tid=5"
```

**Expected:**
- ✅ Complete audit information
- ✅ req_time ≈ resp_time (synchronous)
- ✅ txn_status = SUCCESS

### Test 3: Error Scenario
Temporarily break the properties file to test error handling:

```properties
# Comment out the headers
# email.list.headers=SNo,Name,Email,Job title,Company name,First name,Last name
```

**Expected:**
- ✅ 500 Internal Server Error
- ✅ Audit log created with FAILED status
- ✅ Error message in statusMsg

## Comparison: Sync vs Async

### Synchronous (emailListHeaders)
```
Client Request → AOP (Create TID) → Process → Update Audit → Return Response
                 ↓                                            ↑
                 └────────────── Same Thread ────────────────┘
Time: ~10ms
```

### Asynchronous (add/import-excel)
```
Client Request → AOP (Create TID) → Return ACK (with TID)
                 ↓
                 └→ Background Thread → Process → Update Audit
                 
Client polls: /enquiry?tid=X to check status
Time: Immediate ACK, processing in background
```

## When to Use Each Pattern

### Use Synchronous (like emailListHeaders)
- ✅ Quick operations (< 1 second)
- ✅ Simple data retrieval
- ✅ No heavy processing
- ✅ Client needs immediate result

### Use Asynchronous (like add/import-excel)
- ✅ Long-running operations (> 5 seconds)
- ✅ Heavy processing (Excel parsing, bulk inserts)
- ✅ Don't want to block client
- ✅ Client can poll for status

## Error Handling

### Audit Log Update Failure
If audit log update fails, it won't affect the response:

```java
private void updateAuditLog(...) {
    try {
        // Update audit log
    } catch (Exception e) {
        // Log error but don't throw
        System.err.println("Failed to update audit log: " + e.getMessage());
    }
}
```

This ensures:
- Client always gets the response
- Audit failure doesn't break the API
- Error is logged for investigation

## Monitoring Queries

### Success Rate
```sql
SELECT 
    COUNT(*) as total_requests,
    SUM(CASE WHEN txn_status = 'SUCCESS' THEN 1 ELSE 0 END) as successful,
    SUM(CASE WHEN txn_status = 'FAILED' THEN 1 ELSE 0 END) as failed,
    ROUND(SUM(CASE WHEN txn_status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM email_audit_req_resp_log
WHERE url = '/api/email-info/emailListHeaders';
```

### Average Response Time
```sql
SELECT 
    AVG(TIMESTAMPDIFF(MILLISECOND, req_time, resp_time)) as avg_response_ms
FROM email_audit_req_resp_log
WHERE url = '/api/email-info/emailListHeaders'
AND resp_time IS NOT NULL;
```

### Requests Per Hour
```sql
SELECT 
    DATE_FORMAT(req_time, '%Y-%m-%d %H:00:00') as hour,
    COUNT(*) as request_count
FROM email_audit_req_resp_log
WHERE url = '/api/email-info/emailListHeaders'
GROUP BY DATE_FORMAT(req_time, '%Y-%m-%d %H:00:00')
ORDER BY hour DESC;
```

## Compilation Status

✅ **BUILD SUCCESS** - All changes compiled successfully

## Files Modified

1. **EmailInfoController.java**
   - Added `@TrackTransaction` annotation
   - Added TID retrieval and audit log update
   - Added error handling with audit logging

## Summary

The `/api/email-info/emailListHeaders` endpoint now:
- ✅ Generates TID automatically
- ✅ Stores audit log in database
- ✅ Remains synchronous (no async processing)
- ✅ Returns response immediately
- ✅ Tracks all requests for monitoring
- ✅ Handles errors gracefully

This provides full audit trail without the complexity of async processing, perfect for quick read operations! 🎉
