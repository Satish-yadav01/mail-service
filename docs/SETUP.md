# Quick Setup Guide

## For New Team Members

When you clone this repository for the first time, follow these steps:

### 1. Clone the Repository
```bash
git clone <repository-url>
cd mail-service
```

### 2. Setup Database Credentials
```bash
# Copy the example environment file
cp .env.example .env

# Edit .env with your actual database credentials
# Use your favorite text editor (nano, vim, vscode, etc.)
nano .env
```

### 3. Update .env File
Replace the example values with your actual credentials:
```properties
DB_URL=jdbc:mysql://your-database-host:3306/your-database-name?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 4. Build and Run
```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

### 5. Verify
The application should start successfully on `http://localhost:8080`

## Important Notes

- ✅ The `.env` file is gitignored and will NOT be committed
- ✅ Your credentials stay on your local machine only
- ✅ Each team member can use different database credentials
- ✅ No code changes needed to switch between environments

## Troubleshooting

### Application won't start / Database connection error
- Check your `.env` file exists in the project root
- Verify the database credentials are correct
- Ensure the database server is running and accessible
- Check the database URL format is correct

### .env file not found
```bash
# Make sure you're in the project root directory
pwd
# Should show: /path/to/mail-service

# Copy the example file
cp .env.example .env
```

### Want to use different database for testing
Just update your local `.env` file - it won't affect other team members!

## For Production Deployment

Don't use `.env` files in production. Instead:
- Set environment variables directly in your deployment platform
- Use secrets management services (AWS Secrets Manager, Azure Key Vault, etc.)
- Configure environment variables in your CI/CD pipeline

## Need Help?

- Check `README.md` for detailed setup instructions
- Check `SECURITY.md` for security guidelines
- Contact the project maintainer
