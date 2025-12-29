-- Seed data for MegaBank Payment Gateway
-- This data is loaded automatically when the Docker container starts

-- Insert test users
INSERT INTO users (username, password, role, email, is_active) VALUES
    ('testuser', 'test123', 'USER', 'test@megabank.com', 1),
    ('apiuser', 'api123', 'API', 'api@megabank.com', 1);

-- Insert test API keys
INSERT INTO api_keys (key_value, description, is_active) VALUES
    ('docker_test_key', 'Docker test environment key', 1),
    ('docker_admin_key', 'Docker admin key', 1);

-- Insert test customers
INSERT INTO customers (customer_id, first_name, last_name, email, phone, ssn, password, card_number, cvv, card_expiry, is_vip, created_date) VALUES
    ('CUST_DOCKER_001', 'John', 'Doe', 'john.doe@example.com', '555-0101', '123-45-6789', 'password123', '4111111111111111', '123', '12/25', 0, NOW()),
    ('CUST_DOCKER_002', 'Jane', 'Smith', 'jane.smith@example.com', '555-0102', '987-65-4321', 'password456', '5555555555554444', '456', '06/26', 1, NOW()),
    ('CUST_DOCKER_003', 'Bob', 'Johnson', 'bob.johnson@example.com', '555-0103', '456-78-9012', 'password789', '378282246310005', '789', '09/27', 0, NOW());

-- Insert test merchants
INSERT INTO merchants (merchant_id, name, api_key, secret_key, webhook_url, is_active, fee_percentage, contact_email) VALUES
    ('MERCHANT_TEST_001', 'Test Store Inc', 'merch_test_key_001', 'merch_secret_001', 'http://localhost:8080/webhook', 1, 2.50, 'merchant@teststore.com'),
    ('MERCHANT_TEST_002', 'Demo Shop LLC', 'merch_test_key_002', 'merch_secret_002', 'http://localhost:8080/webhook', 1, 2.75, 'merchant@demoshop.com');

-- Insert some test transactions
INSERT INTO transactions (id, card_number, cvv, expiry_date, amount, currency, status, merchant_id, customer_id, customer_email, ip_address, risk_score, created_date) VALUES
    ('TXN_DOCKER_001', '4111111111111111', '123', '12/25', 99.99, 'USD', 1, 'MERCHANT_TEST_001', 'CUST_DOCKER_001', 'john.doe@example.com', '127.0.0.1', 'LOW', NOW()),
    ('TXN_DOCKER_002', '5555555555554444', '456', '06/26', 149.99, 'USD', 1, 'MERCHANT_TEST_001', 'CUST_DOCKER_002', 'jane.smith@example.com', '127.0.0.1', 'LOW', NOW()),
    ('TXN_DOCKER_003', '378282246310005', '789', '09/27', 249.99, 'USD', 2, 'MERCHANT_TEST_002', 'CUST_DOCKER_003', 'bob.johnson@example.com', '127.0.0.1', 'MEDIUM', NOW());

-- Insert test blacklisted card
INSERT INTO blacklisted_cards (card_number, reason, added_date, added_by) VALUES
    ('4000000000000002', 'Fraudulent activity detected', NOW(), 'SYSTEM');

-- Output confirmation
SELECT 'Seed data loaded successfully' AS Status;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS customer_count FROM customers;
SELECT COUNT(*) AS transaction_count FROM transactions;
SELECT COUNT(*) AS merchant_count FROM merchants;
