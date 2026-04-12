# Mail Service API

A Spring Boot application for managing email contacts with Excel import functionality and direct API insertion.

## Features

- Import emails from Excel files (.xlsx, .xls)
- Add emails directly via REST API
- Automatic duplicate email detection
- Email validation and status tracking
- MySQL database integration
- Automatic timestamp management

## Database Schema

### Email Table Structure
- **id**: Primary Key (Auto-generated)
- **firstName**: First name of the contact
- **lastName**: Last name of the contact
- **email**: Email address (Unique, Required)
- **emailStatus**: Status (VERIFIED/UNVERIFIED)
- **jobTitle**: Job title
- **companyName**: Company name
- **companyDomain**: Company domain
- **location**: Location
- **createdAt**: Automatically set on creation
- **updatedAt**: Automatically updated on modification

## Setup Instructions

### 1. Database Configuration

This project uses environment variables for secure credential management.

**First-time setup:**

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` file with your actual database credentials:
   ```properties
   DB_URL=jdbc:mysql://your-host:3306/your-database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   ```

3. The `.env` file is gitignored and will NOT be committed to the repository.

**Running the application:**

The application will automatically use environment variables from the `.env` file. If not set, it will use default values (localhost:3306/mail_service with root/password).

### 2. Build and Run

```bash
# Build the project
./mvnw clean install

# Run the application (will use .env variables)
./mvnw spring-boot:run
```

**Alternative: Set environment variables manually**

For IntelliJ IDEA:
1. Run → Edit Configurations
2. Add environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

For command line:
```bash
export DB_URL="jdbc:mysql://your-host:3306/your-database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Import Emails from Excel

**Endpoint**: `POST /api/emails/import-excel`

**Description**: Upload an Excel file containing email data. Extra columns in the Excel file will be ignored.

**Request**:
- Method: POST
- Content-Type: multipart/form-data
- Parameter: `file` (Excel file)

**Excel File Format**:
The Excel file should have a header row with the following column names (case-insensitive):
- firstName or first name
- lastName or last name
- email (required)
- emailStatus or email status (VERIFIED/UNVERIFIED)
- jobTitle or job title
- companyName or company name
- companyDomain or company domain
- location

**Example using cURL**:
```bash
curl -X POST http://localhost:8080/api/emails/import-excel \
  -F "file=@/path/to/emails.xlsx"
```

**Example using Postman**:
1. Select POST method
2. Enter URL: `http://localhost:8080/api/emails/import-excel`
3. Go to Body tab
4. Select form-data
5. Add key: `file`, type: File
6. Choose your Excel file

**Response**:
```json
{
  "success": true,
  "message": "Processed 10 emails: 8 successful, 2 failed",
  "totalProcessed": 10,
  "successCount": 8,
  "failureCount": 2
}
```

### 2. Add Emails Directly

**Endpoint**: `POST /api/emails/add`

**Description**: Add emails directly via JSON payload. Extra fields in the request will be ignored.

**Request**:
- Method: POST
- Content-Type: application/json
- Body: Array of email objects

**Example using cURL**:
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
    },
    {
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@example.com",
      "emailStatus": "UNVERIFIED",
      "jobTitle": "Product Manager",
      "companyName": "Innovation Inc",
      "companyDomain": "innovation.com",
      "location": "San Francisco, USA"
    }
  ]'
```

**Example using Postman**:
1. Select POST method
2. Enter URL: `http://localhost:8080/api/emails/add`
3. Go to Body tab
4. Select raw and JSON
5. Enter the JSON array

**Response**:
```json
{
  "success": true,
  "message": "Processed 2 emails: 2 successful, 0 failed",
  "totalProcessed": 2,
  "successCount": 2,
  "failureCount": 0
}
```

## Important Notes

1. **Email Uniqueness**: The email field is unique. Duplicate emails will be rejected.
2. **Default Status**: If emailStatus is not provided, it defaults to "UNVERIFIED".
3. **Extra Fields**: Any extra fields in Excel or JSON requests that don't match the entity fields will be automatically ignored.
4. **Required Field**: Only the email field is required. All other fields are optional.
5. **Timestamps**: createdAt and updatedAt are automatically managed by the system.

## Error Handling

The API provides detailed error messages:
- Invalid file format
- Empty file or request
- Duplicate emails
- Database errors
- Processing errors

## Testing

### Sample Excel File Structure

| firstName | lastName | email | emailStatus | jobTitle | companyName | companyDomain | location | extraColumn |
|-----------|----------|-------|-------------|----------|-------------|---------------|----------|-------------|
| John | Doe | john@example.com | VERIFIED | Engineer | TechCorp | techcorp.com | NY | ignored |
| Jane | Smith | jane@example.com | UNVERIFIED | Manager | InnoInc | innoinc.com | CA | ignored |

Note: The "extraColumn" will be ignored during processing.

## Technologies Used

- Spring Boot 4.0.5
- Spring Data JPA
- MySQL
- Apache POI (Excel processing)
- Lombok
- Hibernate

## License

This project is for demonstration purposes.
