# Async Email Import API Documentation

## Overview
The email import operations have been optimized to use asynchronous processing. This prevents long-running operations from blocking the server and provides better scalability.

## Architecture

### Components

1. **EmailAuditLog Entity**: Tracks all transactions with unique TID
2. **TransactionIdAspect (AOP)**: Automatically generates TID before controller execution
3. **AsyncEmailService**: Handles async processing of emails
4. **EmailController**: Updated endpoints with async support

### Database Table: email_audit_req_resp_log

| Column | Type | Description |
|--------|------|-------------|
| tid | VARCHAR(36) PK | Unique transaction ID (UUID) |
| req | TEXT | Request details |
| resp | TEXT | Response JSON |
| reqTime | TIMESTAMP | Request received time (auto-generated) |
| respTime | TIMESTAMP | Response completion time |
| updatedAt | TIMESTAMP | Last update time (auto-updated) |
| statusCode | INT | HTTP status code |
| statusMsg | VARCHAR | Status message |
| txnStatus | ENUM | Transaction status: PENDING, SUCCESS, FAILED |

## API Endpoints

### 1. Import Excel (Async)
**POST** `/api/emails/import-excel`

**Request:**
- Content-Type: `multipart/form-data`
- Parameter: `file` (Excel file)

**Response (202 Accepted):**
```json
{
  "tid": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Request accepted and processing. Use /api/emails/enquiry?tid=550e8400-e29b-41d4-a716-446655440000 to check status",
  "status": "PENDING"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@emails.xlsx"
```

---

### 2. Add Emails (Async)
**POST** `/api/emails/add`

**Request:**
- Content-Type: `application/json`
- Body: Array of email objects

```json
[
  {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "emailStatus": "VERIFIED",
    "jobTitle": "Software Engineer",
    "companyName": "Tech Corp",
    "companyDomain": "techcorp.com",
    "location": "San Francisco, CA"
  }
]
```

**Response (202 Accepted):**
```json
{
  "tid": "660e8400-e29b-41d4-a716-446655440001",
  "message": "Request accepted and processing. Use /api/emails/enquiry?tid=660e8400-e29b-41d4-a716-446655440001 to check status",
  "status": "PENDING"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "emailStatus": "VERIFIED",
      "jobTitle": "Software Engineer",
      "companyName": "Tech Corp",
      "companyDomain": "techcorp.com",
      "location": "San Francisco, CA"
    }
  ]'
```

---

### 3. Enquiry Status
**GET** `/api/emails/enquiry?tid={tid}`

**Request:**
- Query Parameter: `tid` (Transaction ID)

**Response (200 OK) - Pending:**
```json
{
  "tid": "550e8400-e29b-41d4-a716-446655440000",
  "txnStatus": "PENDING",
  "statusCode": 202,
  "statusMsg": "Request accepted and processing",
  "resp": null,
  "reqTime": "2024-01-15T10:30:00",
  "respTime": null
}
```

**Response (200 OK) - Success:**
```json
{
  "tid": "550e8400-e29b-41d4-a716-446655440000",
  "txnStatus": "SUCCESS",
  "statusCode": 200,
  "statusMsg": "Processing completed successfully",
  "resp": "{\"success\":true,\"message\":\"Processed 100 emails: 95 successful, 5 failed\",\"totalProcessed\":100,\"successCount\":95,\"failureCount\":5}",
  "reqTime": "2024-01-15T10:30:00",
  "respTime": "2024-01-15T10:30:45"
}
```

**Response (200 OK) - Failed:**
```json
{
  "tid": "550e8400-e29b-41d4-a716-446655440000",
  "txnStatus": "FAILED",
  "statusCode": 500,
  "statusMsg": "Processing failed: Invalid file format",
  "resp": "{\"success\":false,\"message\":\"Error: Invalid file format\",\"totalProcessed\":0,\"successCount\":0,\"failureCount\":0}",
  "reqTime": "2024-01-15T10:30:00",
  "respTime": "2024-01-15T10:30:05"
}
```

**Response (404 Not Found):**
```json
{
  "tid": "invalid-tid",
  "txnStatus": null,
  "statusCode": 404,
  "statusMsg": "Transaction not found",
  "resp": null,
  "reqTime": null,
  "respTime": null
}
```

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/emails/enquiry?tid=550e8400-e29b-41d4-a716-446655440000"
```

---

## Workflow

1. **Client sends request** to `/import-excel` or `/add`
2. **AOP intercepts** and generates unique TID
3. **Audit log created** with status PENDING
4. **Immediate response** returned to client with TID
5. **Async processing** starts in background thread
6. **Client polls** `/enquiry?tid={tid}` to check status
7. **Audit log updated** when processing completes (SUCCESS/FAILED)

## Benefits

1. **Non-blocking**: Server thread is released immediately
2. **Scalability**: Can handle multiple large imports concurrently
3. **Traceability**: Complete audit trail of all transactions
4. **User Experience**: Client gets immediate acknowledgment
5. **Error Handling**: Failures are tracked and can be queried

## Transaction Status Flow

```
PENDING → SUCCESS
        ↘ FAILED
```

## Configuration

### Async Thread Pool Settings (application.properties)
```properties
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
spring.task.execution.thread-name-prefix=async-
```

## Error Handling

- **Validation errors**: Return 400 Bad Request immediately
- **Processing errors**: Captured in audit log with FAILED status
- **System errors**: Return 500 Internal Server Error

## Best Practices

1. **Polling Interval**: Poll every 2-5 seconds for status updates
2. **Timeout**: Set reasonable timeout (e.g., 5 minutes) for large imports
3. **Retry Logic**: Implement exponential backoff for enquiry requests
4. **TID Storage**: Store TID on client side for status tracking

## Example Client Implementation

```javascript
async function importEmails(file) {
  // Step 1: Submit import request
  const response = await fetch('/api/emails/import-excel', {
    method: 'POST',
    body: formData
  });
  
  const { tid } = await response.json();
  console.log('Transaction ID:', tid);
  
  // Step 2: Poll for status
  let status = 'PENDING';
  while (status === 'PENDING') {
    await sleep(3000); // Wait 3 seconds
    
    const statusResponse = await fetch(`/api/emails/enquiry?tid=${tid}`);
    const statusData = await statusResponse.json();
    
    status = statusData.txnStatus;
    
    if (status === 'SUCCESS') {
      console.log('Import completed:', JSON.parse(statusData.resp));
    } else if (status === 'FAILED') {
      console.error('Import failed:', statusData.statusMsg);
    }
  }
}
```

## Monitoring

Monitor the following metrics:
- Average processing time per transaction
- Success/failure rates
- Pending transaction count
- Thread pool utilization
