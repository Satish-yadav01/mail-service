# Sample Excel Template for Email Import

## Excel File Structure

Create an Excel file (.xlsx or .xls) with the following columns in the first row (header):

### Required Column:
- **email** - The email address (must be unique)

### Optional Columns:
- **firstName** or **first name** - First name of the contact
- **lastName** or **last name** - Last name of the contact
- **emailStatus** or **email status** - Status: VERIFIED or UNVERIFIED
- **jobTitle** or **job title** - Job title
- **companyName** or **company name** - Company name
- **companyDomain** or **company domain** - Company domain
- **location** - Location

## Sample Data

| firstName | lastName | email | emailStatus | jobTitle | companyName | companyDomain | location |
|-----------|----------|-------|-------------|----------|-------------|---------------|----------|
| John | Doe | john.doe@techcorp.com | VERIFIED | Software Engineer | Tech Corp | techcorp.com | New York, USA |
| Jane | Smith | jane.smith@innovation.com | UNVERIFIED | Product Manager | Innovation Inc | innovation.com | San Francisco, USA |
| Bob | Johnson | bob.johnson@startup.io | VERIFIED | CTO | Startup IO | startup.io | Austin, USA |
| Alice | Williams | alice.w@enterprise.com | VERIFIED | Data Scientist | Enterprise Co | enterprise.com | Seattle, USA |

## Important Notes:

1. **Column Names**: Column names are case-insensitive. You can use "firstName", "firstname", "FirstName", or "first name".

2. **Extra Columns**: You can have additional columns in your Excel file. They will be automatically ignored during import.

3. **Email Status**: Valid values are "VERIFIED" or "UNVERIFIED". If not provided or invalid, it defaults to "UNVERIFIED".

4. **Empty Cells**: Empty cells are allowed for optional fields.

5. **Duplicate Emails**: If an email already exists in the database, it will be skipped and counted as a failure.

## Example with Extra Columns (Will be Ignored)

| firstName | lastName | email | emailStatus | jobTitle | companyName | companyDomain | location | phone | notes | customField |
|-----------|----------|-------|-------------|----------|-------------|---------------|----------|-------|-------|-------------|
| John | Doe | john@example.com | VERIFIED | Engineer | TechCorp | techcorp.com | NY | 555-1234 | VIP | Extra data |

In this example, the columns "phone", "notes", and "customField" will be ignored during import.
