-- MegaBank Payment Gateway Database Schema
-- Created: 2003
-- Last updated: Probably 2010
--
-- WARNING: Do not modify schema without consulting the mainframe team
-- The COBOL copybooks must match this schema!

-- Customers table
-- Stores everything about customers including things we shouldn't
CREATE TABLE customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    ssn VARCHAR(11),  -- Storing SSN in plain text!
    date_of_birth VARCHAR(20),  -- Should be DATE type
    password VARCHAR(100),  -- Plain text password!
    password_hint VARCHAR(255),  -- Security hint
    security_question VARCHAR(255),
    security_answer VARCHAR(255),  -- Plain text!
    pin VARCHAR(4),  -- Plain text PIN!
    credit_limit VARCHAR(20),  -- Should be DECIMAL
    address VARCHAR(500),
    card_number VARCHAR(20),  -- Full card number stored!
    cvv VARCHAR(4),  -- CVV STORED! PCI VIOLATION!
    card_expiry VARCHAR(10),
    is_vip TINYINT DEFAULT 0,
    vip_discount DECIMAL(5,2),
    risk_level INT DEFAULT 1,
    is_blacklisted TINYINT DEFAULT 0,
    blacklist_reason VARCHAR(255),
    failed_login_attempts INT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0,  -- Soft delete - data never actually removed!
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    modified_date DATETIME,
    modified_by VARCHAR(50),
    preferences TEXT,
    notes TEXT,  -- Can contain anything!
    internal_code VARCHAR(50),
    INDEX idx_email (email),
    INDEX idx_ssn (ssn),  -- SSN indexed for fast lookup!
    INDEX idx_card (card_number)  -- Card number indexed!
);

-- Transactions table
CREATE TABLE transactions (
    id VARCHAR(50) PRIMARY KEY,
    card_number VARCHAR(20),  -- Full card number!
    cvv VARCHAR(4),  -- CVV stored! MAJOR PCI VIOLATION!
    expiry_date VARCHAR(10),
    amount DECIMAL(15,2),
    currency VARCHAR(3) DEFAULT 'USD',
    status INT DEFAULT 0,  -- Magic numbers for status
    merchant_id VARCHAR(50),
    customer_id VARCHAR(50),
    customer_email VARCHAR(255),
    customer_ssn VARCHAR(11),  -- SSN in transactions!
    cardholder_name VARCHAR(255),
    billing_address VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),
    risk_score VARCHAR(10),
    is_3d_secure TINYINT,
    processor_response TEXT,
    refund_reason VARCHAR(255),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    modified_date DATETIME,
    temp_debug_data TEXT,  -- Debug data in production!
    field1 VARCHAR(255),  -- Mystery fields
    field2 VARCHAR(255),
    reserved1 VARCHAR(255),
    reserved2 VARCHAR(255),
    legacy_code VARCHAR(50),
    INDEX idx_card (card_number),
    INDEX idx_customer (customer_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status),
    INDEX idx_created (created_date),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Blacklisted cards table
CREATE TABLE blacklisted_cards (
    card_number VARCHAR(20) PRIMARY KEY,  -- Full card number!
    reason VARCHAR(255),
    added_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(50)
);

-- Users table (for admin access)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(100),  -- Plain text!
    role VARCHAR(20),
    email VARCHAR(255),
    is_active TINYINT DEFAULT 1,
    last_login DATETIME,
    failed_login_attempts INT DEFAULT 0,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin users with weak passwords!
INSERT INTO users (username, password, role, email) VALUES
    ('admin', 'admin123', 'ADMIN', 'admin@megabank.com'),
    ('superadmin', 'MegaBank2005!', 'SUPER_ADMIN', 'super@megabank.com'),
    ('developer', 'letmein', 'DEVELOPER', 'dev@megabank.com'),
    ('batch', 'batch123', 'SERVICE', 'batch@megabank.com'),
    ('support', 'support', 'SUPPORT', 'support@megabank.com');

-- API Keys table
CREATE TABLE api_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    key_value VARCHAR(100) UNIQUE,
    description VARCHAR(255),
    is_active TINYINT DEFAULT 1,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_date DATETIME  -- Often NULL, meaning never expires
);

-- Insert some API keys
INSERT INTO api_keys (key_value, description) VALUES
    ('ak_live_megabank_2015_prod_key', 'Production API key'),
    ('ak_test_megabank_development', 'Development key'),
    ('ak_internal_debug_key_12345', 'Internal debugging'),
    ('sk_backdoor_master_key', 'Master key - DO NOT SHARE');

-- Audit log table (rarely used)
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id VARCHAR(50),
    old_value TEXT,  -- Could contain sensitive data!
    new_value TEXT,  -- Could contain sensitive data!
    user_id VARCHAR(50),
    ip_address VARCHAR(50),
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Session table (for tracking logins)
CREATE TABLE sessions (
    token VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50),
    role VARCHAR(20),
    created BIGINT,
    expiry BIGINT,
    data TEXT  -- Session data stored as JSON
);

-- Merchants table
CREATE TABLE merchants (
    merchant_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255),
    api_key VARCHAR(100),  -- API keys in database too
    secret_key VARCHAR(100),  -- More secrets
    webhook_url VARCHAR(500),  -- Could be HTTP, not HTTPS
    is_active TINYINT DEFAULT 1,
    fee_percentage DECIMAL(5,2) DEFAULT 2.50,
    settlement_account VARCHAR(50),
    contact_email VARCHAR(255),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Debug/temp table that shouldn't exist in production
CREATE TABLE debug_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    request_data TEXT,
    response_data TEXT,
    full_card_number VARCHAR(20),  -- Full card!
    cvv VARCHAR(4),  -- CVV!
    password VARCHAR(100),  -- Passwords logged!
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- End of schema
--
-- Notes:
-- - All passwords are stored in plain text (TODO since 2005)
-- - CVV is stored which violates PCI-DSS
-- - SSN stored without encryption
-- - No data masking
-- - No row-level security
-- - Audit log optional and often disabled
-- - Debug table contains sensitive data
