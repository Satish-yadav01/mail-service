# Email Info Controller - Email List Headers Endpoint

## Overview
Created a new controller `EmailInfoController` with an endpoint to retrieve email list headers. The headers are externalized in the properties file for easy configuration.

## New Endpoint

### GET /api/email-info/emailListHeaders

Returns the list of headers for email data display.

**URL:** `http://localhost:8080/api/email-info/emailListHeaders`

**Method:** `GET`

**Authentication:** None (currently)

**Request Parameters:** None

## Response Format

### Success Response (200 OK)

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

### Error Response (500 Internal Server Error)

```json
{
    "success": false,
    "message": "Error retrieving email list headers: [error details]",
    "headers": null
}
```

## Configuration

### application.properties

The headers are configured in the properties file:

```properties
# Email List Headers Configuration
email.list.headers=SNo,Name,Email,Job title,Company name,First name,Last name
```

### How to Modify Headers

To change the headers, simply update the `email.list.headers` property in `application.properties`:

**Example 1: Add more headers**
```properties
email.list.headers=SNo,Name,Email,Job title,Company name,First name,Last name,Phone,Department
```

**Example 2: Change order**
```properties
email.list.headers=SNo,First name,Last name,Email,Company name,Job title,Name
```

**Example 3: Remove headers**
```properties
email.list.headers=SNo,Name,Email,Company name
```

**Note:** After changing the properties file, restart the application for changes to take effect.

## Implementation Details

### Files Created

1. **EmailHeadersResponse.java** - DTO for headers response
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailHeadersResponse {
    private boolean success;
    private String message;
    private List<String> headers;
}
```

2. **EmailInfoController.java** - Controller with headers endpoint
```java
@RestController
@RequestMapping("/api/email-info")
public class EmailInfoController {
    
    @Value("${email.list.headers}")
    private String emailListHeaders;
    
    @GetMapping("/emailListHeaders")
    public ResponseEntity<EmailHeadersResponse> getEmailListHeaders() {
        // Returns headers from properties file
    }
}
```

### Files Modified

1. **application.properties** - Added email list headers configuration

## Testing

### Using cURL

```bash
curl -X GET http://localhost:8080/api/email-info/emailListHeaders
```

### Using Postman

1. **Method:** GET
2. **URL:** `http://localhost:8080/api/email-info/emailListHeaders`
3. **Headers:** None required
4. **Body:** None

### Using Browser

Simply navigate to:
```
http://localhost:8080/api/email-info/emailListHeaders
```

## Example Response

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

## Use Cases

### 1. Frontend Table Display
Use the headers to dynamically create table columns:

```javascript
// Fetch headers
const response = await fetch('/api/email-info/emailListHeaders');
const data = await response.json();

// Create table headers
const headers = data.headers;
headers.forEach(header => {
    const th = document.createElement('th');
    th.textContent = header;
    tableHeaderRow.appendChild(th);
});
```

### 2. Excel Export
Use headers for Excel column names:

```javascript
const { headers } = await getEmailListHeaders();
const worksheet = XLSX.utils.json_to_sheet(emailData, { header: headers });
```

### 3. CSV Generation
Use headers for CSV file:

```javascript
const { headers } = await getEmailListHeaders();
const csv = [
    headers.join(','),
    ...emailData.map(row => headers.map(h => row[h]).join(','))
].join('\n');
```

### 4. Form Field Labels
Use headers to generate form labels dynamically:

```javascript
const { headers } = await getEmailListHeaders();
headers.forEach(header => {
    const label = document.createElement('label');
    label.textContent = header;
    form.appendChild(label);
});
```

## Benefits

### 1. **Externalized Configuration**
- Headers are in properties file, not hardcoded
- Easy to modify without code changes
- No recompilation needed (just restart)

### 2. **Centralized Management**
- Single source of truth for headers
- Consistent across the application
- Easy to maintain

### 3. **Flexibility**
- Can change headers per environment (dev, staging, prod)
- Can add/remove headers easily
- Can reorder headers without code changes

### 4. **Dynamic Frontend**
- Frontend can fetch headers dynamically
- No need to hardcode headers in frontend
- Automatic sync between backend and frontend

## Environment-Specific Configuration

You can have different headers for different environments:

### application-dev.properties
```properties
email.list.headers=SNo,Name,Email,Job title,Company name,First name,Last name,Debug Info
```

### application-prod.properties
```properties
email.list.headers=SNo,Name,Email,Job title,Company name,First name,Last name
```

## Advanced Configuration

### Using YAML (application.yml)

If you prefer YAML format:

```yaml
email:
  list:
    headers: SNo,Name,Email,Job title,Company name,First name,Last name
```

### Using List Format

For better readability in YAML:

```yaml
email:
  list:
    headers:
      - SNo
      - Name
      - Email
      - Job title
      - Company name
      - First name
      - Last name
```

Then update the controller:

```java
@Value("${email.list.headers}")
private List<String> emailListHeaders;

@GetMapping("/emailListHeaders")
public ResponseEntity<EmailHeadersResponse> getEmailListHeaders() {
    EmailHeadersResponse response = new EmailHeadersResponse(
            true,
            "Email list headers retrieved successfully",
            emailListHeaders  // Already a List
    );
    return ResponseEntity.ok(response);
}
```

## Error Handling

The endpoint handles errors gracefully:

1. **Missing Property:** Returns error with message
2. **Invalid Format:** Returns error with details
3. **Empty Headers:** Returns empty list with success

## Integration with Existing Endpoints

This endpoint complements the existing email endpoints:

1. **GET /api/email-info/emailListHeaders** - Get headers
2. **POST /api/emails/add** - Add emails with these fields
3. **POST /api/emails/import-excel** - Import Excel with these columns
4. **GET /api/emails/enquiry** - Check import status

## Postman Collection Update

Add this to your Postman collection:

```json
{
    "name": "Get Email List Headers",
    "request": {
        "method": "GET",
        "header": [],
        "url": {
            "raw": "http://localhost:8080/api/email-info/emailListHeaders",
            "protocol": "http",
            "host": ["localhost"],
            "port": "8080",
            "path": ["api", "email-info", "emailListHeaders"]
        }
    }
}
```

## Compilation Status

✅ **BUILD SUCCESS** - All files compiled successfully

## Files Summary

### Created
1. `/dto/EmailHeadersResponse.java` - Response DTO
2. `/controller/EmailInfoController.java` - New controller

### Modified
1. `/resources/application.properties` - Added headers configuration

## Next Steps

1. Restart the application
2. Test the endpoint using cURL or Postman
3. Integrate with your frontend application
4. Customize headers as needed in properties file

## Example Frontend Integration

```javascript
// React Example
import { useEffect, useState } from 'react';

function EmailTable() {
    const [headers, setHeaders] = useState([]);
    const [emails, setEmails] = useState([]);

    useEffect(() => {
        // Fetch headers
        fetch('/api/email-info/emailListHeaders')
            .then(res => res.json())
            .then(data => setHeaders(data.headers));
        
        // Fetch emails
        fetch('/api/emails')
            .then(res => res.json())
            .then(data => setEmails(data));
    }, []);

    return (
        <table>
            <thead>
                <tr>
                    {headers.map(header => (
                        <th key={header}>{header}</th>
                    ))}
                </tr>
            </thead>
            <tbody>
                {emails.map(email => (
                    <tr key={email.id}>
                        {headers.map(header => (
                            <td key={header}>{email[header]}</td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
```

Your new endpoint is ready to use! 🎉
