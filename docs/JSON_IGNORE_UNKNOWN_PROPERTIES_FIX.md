# JSON Ignore Unknown Properties Fix

## Problem

The application was throwing an error when receiving JSON with extra fields that don't exist in the DTO:

```
org.springframework.http.converter.HttpMessageNotReadableException: 
JSON parse error: Unrecognized field "phoneNumber" 
(class com.satish.mailservice.dto.EmailRequest), not marked as ignorable
```

**Example Request that caused the error:**
```json
[
  {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phoneNumber": "123-456-7890"  // ❌ This field doesn't exist in EmailRequest
  }
]
```

## Solution

Added `@JsonIgnoreProperties(ignoreUnknown = true)` annotation to all DTOs. This tells Jackson to ignore any JSON properties that don't have corresponding fields in the Java class.

## Changes Made

### 1. EmailRequest.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Added
public class EmailRequest {
    private String firstName;
    private String lastName;
    private String email;
    private EmailStatus emailStatus;
    private String jobTitle;
    private String companyName;
    private String companyDomain;
    private String location;
}
```

### 2. EmailResponse.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Added
public class EmailResponse {
    private boolean success;
    private String message;
    private int totalProcessed;
    private int successCount;
    private int failureCount;
}
```

### 3. AsyncResponse.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Added
public class AsyncResponse {
    private Long tid;
    private String message;
    private String status;
}
```

### 4. EnquiryResponse.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ✅ Added
public class EnquiryResponse {
    private Long tid;
    private String method;
    private String url;
    private TransactionStatus txnStatus;
    private Integer statusCode;
    private String statusMsg;
    private EmailResponse resp;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reqTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime respTime;
}
```

## Benefits

### 1. **Backward Compatibility**
- API won't break if clients send extra fields
- Useful when clients have newer versions with additional fields

### 2. **Forward Compatibility**
- You can add fields to the client without updating the server immediately
- Gradual rollout of new features

### 3. **Flexibility**
- Clients can send data from different sources with varying schemas
- Excel imports with extra columns won't fail

### 4. **Better Error Handling**
- No more cryptic Jackson deserialization errors
- Application continues to work with the fields it knows about

## How It Works

**Before (Without annotation):**
```json
// Request
{
  "firstName": "John",
  "email": "john@example.com",
  "phoneNumber": "123-456-7890"
}

// Result: ❌ ERROR - Unrecognized field "phoneNumber"
```

**After (With annotation):**
```json
// Request
{
  "firstName": "John",
  "email": "john@example.com",
  "phoneNumber": "123-456-7890"
}

// Result: ✅ SUCCESS
// EmailRequest object created with:
// - firstName: "John"
// - email: "john@example.com"
// - phoneNumber: ignored (not stored)
```

## Testing

### Test 1: Request with Extra Fields
```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "phoneNumber": "123-456-7890",
      "age": 30,
      "department": "Engineering"
    }
  ]'
```

**Expected Result:** ✅ Success
- Only known fields (firstName, lastName, email) are processed
- Unknown fields (phoneNumber, age, department) are ignored
- No error thrown

### Test 2: Request with Only Known Fields
```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane@example.com"
    }
  ]'
```

**Expected Result:** ✅ Success (works as before)

### Test 3: Excel Import with Extra Columns
If your Excel file has columns like:
- firstName
- lastName
- email
- phoneNumber ← Extra column
- socialMedia ← Extra column

**Result:** ✅ Success
- Known columns are imported
- Extra columns are ignored

## Important Notes

### What Gets Ignored
- Any JSON field that doesn't have a corresponding Java field
- The data is simply not stored, not validated

### What Doesn't Get Ignored
- Required fields validation still works
- Type mismatches still throw errors
- Null values for non-nullable fields still fail

### Example of What Still Fails
```json
{
  "firstName": "John",
  "email": 12345  // ❌ Still fails - wrong type (should be String)
}
```

## Alternative Approaches

If you want more control, you can use:

### 1. Specific Field Ignoring
```java
@JsonIgnoreProperties({"phoneNumber", "age"})
public class EmailRequest {
    // Only ignores specific fields
}
```

### 2. Individual Field Annotation
```java
public class EmailRequest {
    private String firstName;
    
    @JsonIgnore
    private String internalField;  // Never serialized/deserialized
}
```

### 3. Global Configuration
```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
```

## Best Practices

1. ✅ **Use `@JsonIgnoreProperties(ignoreUnknown = true)` for DTOs**
   - Especially for request DTOs that receive external data

2. ✅ **Document your API**
   - Clearly specify which fields are required
   - List all accepted fields

3. ✅ **Validate Important Fields**
   - Use `@NotNull`, `@NotEmpty`, etc. for required fields
   - Don't rely on Jackson for business validation

4. ✅ **Log Unknown Fields (Optional)**
   - If you want to track what's being ignored:
   ```java
   @JsonAnySetter
   public void handleUnknown(String key, Object value) {
       log.warn("Unknown property: {} = {}", key, value);
   }
   ```

## Compilation Status

✅ **BUILD SUCCESS** - All DTOs updated successfully

## Files Modified

1. `/dto/EmailRequest.java` - Added `@JsonIgnoreProperties(ignoreUnknown = true)`
2. `/dto/EmailResponse.java` - Added `@JsonIgnoreProperties(ignoreUnknown = true)`
3. `/dto/AsyncResponse.java` - Added `@JsonIgnoreProperties(ignoreUnknown = true)`
4. `/dto/EnquiryResponse.java` - Added `@JsonIgnoreProperties(ignoreUnknown = true)`

## Summary

The application will now gracefully handle JSON requests with extra fields, making it more robust and flexible. This is especially useful for:
- Excel imports with extra columns
- API evolution and versioning
- Integration with third-party systems
- Client applications with different data models

Your API is now more resilient and won't break when receiving unexpected fields! 🎉
