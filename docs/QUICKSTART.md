# Quick Start Guide - Mail Service

## Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## Step-by-Step Setup

### 1. Setup MySQL Database

```sql
-- Login to MySQL
mysql -u root -p

-- Create database (optional, as the app will create it automatically)
CREATE DATABASE mail_service_db;

-- Verify database
SHOW DATABASES;
```

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

### 3. Build the Project

```bash
# Navigate to project directory
cd /Users/satishyadav/Developer/mail-service/mail-service

# Clean and build
./mvnw clean install
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Verify the Application

Check the console output for:
```
Started MailServiceApplication in X.XXX seconds
```

### 6. Test the Endpoints

#### Option A: Using cURL

**Test 1: Add emails directly**
```bash
curl -X POST http://localhost:8080/api/emails/add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "firstName": "Test",
      "lastName": "User",
      "email": "test@example.com",
      "emailStatus": "VERIFIED",
      "jobTitle": "Developer",
      "companyName": "Test Corp",
      "companyDomain": "testcorp.com",
      "location": "Test City"
    }
  ]'
```

**Test 2: Import from Excel**
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@/path/to/your/emails.xlsx"
```

#### Option B: Using Postman

1. Import the Postman collection: `Mail_Service_API.postman_collection.json`
2. Run the requests from the collection

### 7. Verify Data in Database

```sql
-- Login to MySQL
mysql -u root -p

-- Use the database
USE mail_service_db;

-- Check the emails table
DESCRIBE emails;

-- View all emails
SELECT * FROM emails;
```

## Expected Database Schema

```sql
CREATE TABLE emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    email_status VARCHAR(20),
    job_title VARCHAR(255),
    company_name VARCHAR(255),
    company_domain VARCHAR(255),
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Common Issues and Solutions

### Issue 1: Port 8080 already in use
**Solution**: Change the port in `application.properties`
```properties
server.port=8081
```

### Issue 2: MySQL connection refused
**Solution**: 
- Verify MySQL is running: `mysql --version`
- Check MySQL service: `brew services list` (Mac) or `systemctl status mysql` (Linux)
- Verify credentials in `application.properties`

### Issue 3: File upload size exceeded
**Solution**: The default limit is 10MB. To increase:
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### Issue 4: Duplicate email error
**Solution**: This is expected behavior. The email field is unique. Check existing emails:
```sql
SELECT email FROM emails WHERE email = 'duplicate@example.com';
```

## Testing with Sample Data

### Create a Sample Excel File

Create an Excel file named `sample_emails.xlsx` with this structure:

| firstName | lastName | email | emailStatus | jobTitle | companyName | companyDomain | location |
|-----------|----------|-------|-------------|----------|-------------|---------------|----------|
| John | Doe | john@test.com | VERIFIED | Engineer | TestCo | testco.com | NY |
| Jane | Smith | jane@test.com | UNVERIFIED | Manager | TestInc | testinc.com | CA |

Then upload it:
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@sample_emails.xlsx"
```

## Next Steps

1. Review the API documentation in `README.md`
2. Check the Excel template guide in `EXCEL_TEMPLATE.md`
3. Import the Postman collection for easy testing
4. Customize the application as needed

## Support

For issues or questions, check:
- Application logs in the console
- MySQL error logs
- Spring Boot documentation: https://spring.io/projects/spring-boot
