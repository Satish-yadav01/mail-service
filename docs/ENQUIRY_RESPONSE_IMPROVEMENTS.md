# Enquiry Response Improvements

## Changes Made

### 1. Human-Readable DateTime Format

**Before:**
```json
{
    "reqTime": [2026, 4, 12, 18, 9, 49],
    "respTime": [2026, 4, 12, 18, 9, 51]
}
```

**After:**
```json
{
    "reqTime": "2026-04-12 18:09:49",
    "respTime": "2026-04-12 18:09:51"
}
```

**Implementation:**
Added `@JsonFormat` annotation to `reqTime` and `respTime` fields in `EnquiryResponse`:

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime reqTime;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime respTime;
```

### 2. Response as DTO Instead of String

**Before:**
```json
{
    "resp": "{\"success\":false,\"message\":\"Processed 2 emails: 0 successful, 2 failed\",\"totalProcessed\":2,\"successCount\":0,\"failureCount\":2}"
}
```

**After:**
```json
{
    "resp": {
        "success": false,
        "message": "Processed 2 emails: 0 successful, 2 failed",
        "totalProcessed": 2,
        "successCount": 0,
        "failureCount": 2
    }
}
```

**Implementation:**
1. Changed `resp` field type from `String` to `EmailResponse` in `EnquiryResponse` DTO
2. Added JSON deserialization in the controller's enquiry endpoint
3. Injected `ObjectMapper` to deserialize the stored JSON string

## Complete Updated Response

**New Enquiry Response Format:**
```json
{
    "tid": 1,
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Processing completed successfully",
    "resp": {
        "success": false,
        "message": "Processed 2 emails: 0 successful, 2 failed",
        "totalProcessed": 2,
        "successCount": 0,
        "failureCount": 2
    },
    "reqTime": "2026-04-12 18:09:49",
    "respTime": "2026-04-12 18:09:51"
}
```

## Files Modified

1. **EnquiryResponse.java**
   - Changed `resp` from `String` to `EmailResponse`
   - Added `@JsonFormat` annotations for datetime fields

2. **EmailController.java**
   - Added `ObjectMapper` dependency injection
   - Added JSON deserialization logic in `enquireTransactionStatus()` method

## Benefits

### 1. Better Readability
- DateTime is now in standard format: `yyyy-MM-dd HH:mm:ss`
- Easy to read and understand at a glance
- No need for client-side parsing of array format

### 2. Type Safety
- `resp` is now a proper DTO object instead of a string
- Clients can directly access fields without JSON parsing
- Better IDE support and autocomplete for API consumers

### 3. Cleaner API
- More professional and consistent API response
- Follows REST API best practices
- Easier to document and maintain

## Testing

**Test the enquiry endpoint:**
```bash
curl -X GET "http://localhost:8080/api/emails/enquiry?tid=1"
```

**Expected Response:**
```json
{
    "tid": 1,
    "txnStatus": "SUCCESS",
    "statusCode": 200,
    "statusMsg": "Processing completed successfully",
    "resp": {
        "success": true,
        "message": "Processed 10 emails: 8 successful, 2 failed",
        "totalProcessed": 10,
        "successCount": 8,
        "failureCount": 2
    },
    "reqTime": "2026-04-12 18:15:30",
    "respTime": "2026-04-12 18:15:35"
}
```

## Error Handling

If the stored `resp` JSON cannot be deserialized:
- The `resp` field will be `null`
- The rest of the response will still be returned
- No exception is thrown to the client

## Backward Compatibility

⚠️ **Breaking Change**: This is a breaking change for existing API consumers.

**Migration Guide for API Consumers:**

**Before:**
```javascript
// Client had to parse the resp string
const data = await fetch('/api/emails/enquiry?tid=1').then(r => r.json());
const resp = JSON.parse(data.resp);
console.log(resp.successCount);

// DateTime was an array
const year = data.reqTime[0];
const month = data.reqTime[1];
```

**After:**
```javascript
// Client can directly access resp object
const data = await fetch('/api/emails/enquiry?tid=1').then(r => r.json());
console.log(data.resp.successCount);

// DateTime is a formatted string
const reqTime = data.reqTime; // "2026-04-12 18:09:49"
```

## Compilation Status

✅ **BUILD SUCCESS** - All changes compiled successfully

## Additional Notes

- The datetime format can be customized by changing the pattern in `@JsonFormat`
- Common patterns:
  - `yyyy-MM-dd HH:mm:ss` (current)
  - `yyyy-MM-dd'T'HH:mm:ss` (ISO 8601)
  - `dd/MM/yyyy HH:mm:ss` (European format)
  - `MM/dd/yyyy hh:mm:ss a` (US format with AM/PM)
