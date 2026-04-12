# Security Guidelines

## Database Credentials Management

This project uses **environment variables** to securely manage sensitive database credentials.

## How It Works

### 1. Environment Variables
The application reads database credentials from environment variables:
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

### 2. Configuration Files

**application.properties** (Committed to Git)
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/mail_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:password}
```

The syntax `${ENV_VAR:default_value}` means:
- Use the environment variable if available
- Fall back to the default value if not set

**.env** (NOT committed - in .gitignore)
```properties
DB_URL=jdbc:mysql://your-host:3306/your-database
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**.env.example** (Committed to Git as template)
```properties
DB_URL=jdbc:mysql://mysql-satishdb.alwaysdata.net:3306/satishdb_mail_service
DB_USERNAME=satishdb
DB_PASSWORD=1@Bhishek29072001
```

## Setup for New Developers

When someone clones the repository:

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Update with actual credentials:**
   Edit `.env` with real database credentials

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

The application automatically loads environment variables from `.env` file.

## Production Deployment

For production environments, set environment variables directly:

### Linux/Mac:
```bash
export DB_URL="jdbc:mysql://prod-host:3306/prod-db"
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"
```

### Windows:
```cmd
set DB_URL=jdbc:mysql://prod-host:3306/prod-db
set DB_USERNAME=prod_user
set DB_PASSWORD=secure_password
```

### Docker:
```yaml
environment:
  - DB_URL=jdbc:mysql://prod-host:3306/prod-db
  - DB_USERNAME=prod_user
  - DB_PASSWORD=secure_password
```

### Cloud Platforms:
- **AWS**: Use AWS Secrets Manager or Parameter Store
- **Azure**: Use Azure Key Vault
- **Heroku**: Set Config Vars in dashboard
- **Google Cloud**: Use Secret Manager

## What Gets Committed to Git

✅ **Safe to commit:**
- `application.properties` (with environment variable placeholders)
- `.env.example` (template with example/dummy values)
- `.gitignore` (includes `.env`)

❌ **Never commit:**
- `.env` (contains actual credentials)
- `application-local.properties` (if it contains credentials)
- Any file with real passwords or API keys

## Benefits of This Approach

1. **Security**: Real credentials never go to Git
2. **Flexibility**: Different credentials for dev/staging/prod
3. **Easy Setup**: New developers just copy `.env.example` to `.env`
4. **No Code Changes**: Switch environments without changing code
5. **Team Friendly**: Everyone can use their own local database

## Verification

To verify your setup is secure before committing:

```bash
# Check what will be committed
git status

# Verify .env is ignored
git check-ignore .env

# Should output: .env (meaning it's ignored)
```

## Additional Security Recommendations

1. **Never log credentials** - Ensure logging doesn't expose passwords
2. **Use strong passwords** - Especially for production databases
3. **Rotate credentials regularly** - Change passwords periodically
4. **Limit database access** - Use principle of least privilege
5. **Use SSL/TLS** - Enable secure connections in production
6. **Regular security audits** - Review access logs and permissions

## Emergency: If Credentials Were Committed

If you accidentally committed credentials:

1. **Immediately change the password** on the database
2. **Remove from Git history:**
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch src/main/resources/application.properties" \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. **Force push** (coordinate with team):
   ```bash
   git push origin --force --all
   ```
4. **Notify your team** to re-clone the repository

## Questions?

If you have questions about security setup, refer to:
- README.md for setup instructions
- This SECURITY.md for security details
- Contact the project maintainer
