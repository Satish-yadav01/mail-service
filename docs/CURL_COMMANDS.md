# cURL Commands for Testing Mail Service API

## Prerequisites
- Application running on http://localhost:8080
- MySQL database configured and running

---

## 1. Add Single Email (Direct API)

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
      "location": "New York, USA"
    }
  ]'
```

---

## 2. Add Multiple Emails (Direct API)

```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@example.com",
      "emailStatus": "UNVERIFIED",
      "jobTitle": "Product Manager",
      "companyName": "Innovation Inc",
      "companyDomain": "innovation.com",
      "location": "San Francisco, USA"
    },
    {
      "firstName": "Bob",
      "lastName": "Johnson",
      "email": "bob.johnson@startup.io",
      "emailStatus": "VERIFIED",
      "jobTitle": "CTO",
      "companyName": "Startup IO",
      "companyDomain": "startup.io",
      "location": "Austin, USA"
    },
    {
      "firstName": "Alice",
      "lastName": "Williams",
      "email": "alice.w@enterprise.com",
      "emailStatus": "VERIFIED",
      "jobTitle": "Data Scientist",
      "companyName": "Enterprise Co",
      "companyDomain": "enterprise.com",
      "location": "Seattle, USA"
    }
  ]'
```

---

## 3. Add Email with Extra Fields (Should be Ignored)

```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "Charlie",
      "lastName": "Brown",
      "email": "charlie.brown@test.com",
      "emailStatus": "VERIFIED",
      "jobTitle": "Developer",
      "companyName": "Test Company",
      "companyDomain": "testcompany.com",
      "location": "Boston, USA",
      "phoneNumber": "555-1234",
      "notes": "This field will be ignored",
      "customField": "This will also be ignored",
      "extraData": "Ignored as well"
    }
  ]'
```

---

## 4. Add Email with Minimal Fields

```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "email": "minimal@example.com"
    }
  ]'
```

---

## 5. Import Emails from Excel File

```bash
# Replace /path/to/your/file.xlsx with actual file path
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@/path/to/your/file.xlsx"
```

**Example with specific path:**
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@/Users/satishyadav/Desktop/emails.xlsx"
```

---

## 6. Test Duplicate Email (Should Fail)

```bash
# First, add an email
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "email": "duplicate@example.com",
      "firstName": "First",
      "lastName": "User"
    }
  ]'

# Then try to add the same email again (should fail)
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "email": "duplicate@example.com",
      "firstName": "Second",
      "lastName": "User"
    }
  ]'
```

---

## 7. Test with Invalid File Format (Should Fail)

```bash
# Try to upload a non-Excel file
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@/path/to/document.pdf"
```

---

## 8. Test with Empty Request (Should Fail)

```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[]'
```

---

## 9. Bulk Insert Test (10 Emails)

```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {"email": "user1@test.com", "firstName": "User", "lastName": "One", "emailStatus": "VERIFIED"},
    {"email": "user2@test.com", "firstName": "User", "lastName": "Two", "emailStatus": "UNVERIFIED"},
    {"email": "user3@test.com", "firstName": "User", "lastName": "Three", "emailStatus": "VERIFIED"},
    {"email": "user4@test.com", "firstName": "User", "lastName": "Four", "emailStatus": "VERIFIED"},
    {"email": "user5@test.com", "firstName": "User", "lastName": "Five", "emailStatus": "UNVERIFIED"},
    {"email": "user6@test.com", "firstName": "User", "lastName": "Six", "emailStatus": "VERIFIED"},
    {"email": "user7@test.com", "firstName": "User", "lastName": "Seven", "emailStatus": "VERIFIED"},
    {"email": "user8@test.com", "firstName": "User", "lastName": "Eight", "emailStatus": "UNVERIFIED"},
    {"email": "user9@test.com", "firstName": "User", "lastName": "Nine", "emailStatus": "VERIFIED"},
    {"email": "user10@test.com", "firstName": "User", "lastName": "Ten", "emailStatus": "VERIFIED"}
  ]'
```

---

## Expected Response Format

### Success Response:
```json
{
  "success": true,
  "message": "Processed 3 emails: 3 successful, 0 failed",
  "totalProcessed": 3,
  "successCount": 3,
  "failureCount": 0
}
```

### Partial Success Response:
```json
{
  "success": true,
  "message": "Processed 5 emails: 3 successful, 2 failed",
  "totalProcessed": 5,
  "successCount": 3,
  "failureCount": 2
}
```

### Error Response:
```json
{
  "success": false,
  "message": "Email list is empty",
  "totalProcessed": 0,
  "successCount": 0,
  "failureCount": 0
}
```

---

## Verify Data in Database

After running the above commands, verify the data:

```bash
# Connect to MySQL
mysql -u root -p

# Run queries
USE mail_service_db;
SELECT COUNT(*) FROM emails;
SELECT * FROM emails ORDER BY created_at DESC LIMIT 10;
SELECT email_status, COUNT(*) as count FROM emails GROUP BY email_status;
```

---

## Tips

1. **Pretty Print JSON Response**: Add `| jq` at the end of curl commands (requires jq installation)
   ```bash
   curl -X POST http://localhost:8080/api/emails/add \
     -H "Content-Type: application/json" \
     -d '[{"email":"test@example.com"}]' | jq
   ```

2. **Save Response to File**:
   ```bash
   curl -X POST http://localhost:8080/api/emails/add \
     -H "Content-Type: application/json" \
     -d '[{"email":"test@example.com"}]' > response.json
   ```

3. **Verbose Output** (for debugging):
   ```bash
   curl -v -X POST http://localhost:8080/api/emails/add \
     -H "Content-Type: application/json" \
     -d '[{"email":"test@example.com"}]'
   ```

4. **Check Application Health**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   (Note: Requires Spring Boot Actuator dependency)

---

## Troubleshooting

### Connection Refused
```bash
# Check if application is running
curl http://localhost:8080
```

### Invalid JSON
```bash
# Validate your JSON at https://jsonlint.com/
```

### File Not Found
```bash
# Verify file exists
ls -la /path/to/your/file.xlsx
```

---

## Quick Test Script

Save this as `test_api.sh`:

```bash
#!/bin/bash

echo "Testing Mail Service API..."
echo ""

echo "1. Adding single email..."
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[{"email":"test1@example.com","firstName":"Test","lastName":"User"}]'
echo -e "\n"

echo "2. Adding multiple emails..."
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[{"email":"test2@example.com"},{"email":"test3@example.com"}]'
echo -e "\n"

echo "3. Testing duplicate (should fail)..."
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[{"email":"test1@example.com"}]'
echo -e "\n"

echo "Tests completed!"
```

Run with: `bash test_api.sh`
