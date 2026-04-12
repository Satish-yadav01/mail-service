-- Sample Test Data for Mail Service
-- Run this script after the application has created the tables

USE mail_service_db;

-- Clear existing data (optional)
-- TRUNCATE TABLE emails;

-- Insert sample email records
INSERT INTO emails (first_name, last_name, email, email_status, job_title, company_name, company_domain, location, created_at, updated_at)
VALUES
    ('John', 'Doe', 'john.doe@techcorp.com', 'VERIFIED', 'Software Engineer', 'Tech Corp', 'techcorp.com', 'New York, USA', NOW(), NOW()),
    ('Jane', 'Smith', 'jane.smith@innovation.com', 'UNVERIFIED', 'Product Manager', 'Innovation Inc', 'innovation.com', 'San Francisco, USA', NOW(), NOW()),
    ('Bob', 'Johnson', 'bob.johnson@startup.io', 'VERIFIED', 'CTO', 'Startup IO', 'startup.io', 'Austin, USA', NOW(), NOW()),
    ('Alice', 'Williams', 'alice.w@enterprise.com', 'VERIFIED', 'Data Scientist', 'Enterprise Co', 'enterprise.com', 'Seattle, USA', NOW(), NOW()),
    ('Charlie', 'Brown', 'charlie.b@consulting.com', 'UNVERIFIED', 'Consultant', 'Consulting Group', 'consulting.com', 'Boston, USA', NOW(), NOW()),
    ('Diana', 'Davis', 'diana.davis@finance.com', 'VERIFIED', 'Financial Analyst', 'Finance Corp', 'finance.com', 'Chicago, USA', NOW(), NOW()),
    ('Edward', 'Miller', 'edward.m@marketing.com', 'VERIFIED', 'Marketing Director', 'Marketing Pro', 'marketing.com', 'Los Angeles, USA', NOW(), NOW()),
    ('Fiona', 'Wilson', 'fiona.w@design.com', 'UNVERIFIED', 'UX Designer', 'Design Studio', 'design.com', 'Portland, USA', NOW(), NOW()),
    ('George', 'Moore', 'george.m@sales.com', 'VERIFIED', 'Sales Manager', 'Sales Force', 'sales.com', 'Miami, USA', NOW(), NOW()),
    ('Hannah', 'Taylor', 'hannah.t@hr.com', 'VERIFIED', 'HR Manager', 'HR Solutions', 'hr.com', 'Denver, USA', NOW(), NOW());

-- Verify the data
SELECT COUNT(*) as total_emails FROM emails;
SELECT email_status, COUNT(*) as count FROM emails GROUP BY email_status;
SELECT * FROM emails ORDER BY created_at DESC LIMIT 5;

-- Query examples
-- Find all verified emails
SELECT * FROM emails WHERE email_status = 'VERIFIED';

-- Find emails by company
SELECT * FROM emails WHERE company_name LIKE '%Corp%';

-- Find emails by location
SELECT * FROM emails WHERE location LIKE '%USA%';

-- Find recently added emails (last 24 hours)
SELECT * FROM emails WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);
