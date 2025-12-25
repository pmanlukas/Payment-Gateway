# API Documentation

MegaBank Payment Gateway REST API - Version 1

## Overview

The MegaBank Payment Gateway provides a "REST-ish" API for payment processing. The API was added in 2018 to provide "modern" access to the legacy payment system.

**Base URL**: `http://localhost:8080` (or your deployment URL)

**Authentication**: All endpoints require an `X-API-Key` header

⚠️ **Security Warning**: This API has numerous security issues including:
- Hardcoded API keys
- No rate limiting
- Returns sensitive data (CVV, passwords)
- SQL injection vulnerabilities
- No HTTPS enforcement
- CORS set to `*`

## Authentication

All API requests must include an API key in the header:

```http
X-API-Key: your-api-key-here
```

### Valid API Keys (Hardcoded)

For demonstration purposes, the following API keys are hardcoded:
- `test_key_12345` - Test key
- `prod_key_67890` - Production key (same security as test!)
- `internal_key_admin` - Admin key
- `backdoor_key_debug123` - Debug key (yes, really)

Additional keys can be found in the `api_keys` database table.

## API Endpoints

### 1. Get API Info

Get basic information about the API.

**Endpoint**: `GET /`

**Authentication**: Required

**Request**:
```bash
curl -X GET http://localhost:8080/ \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "name": "MegaBank Payment Gateway API",
  "version": "v1",
  "status": "operational",
  "server": "Mac OS X",
  "java_version": "1.8.0_292"
}
```

### 2. Process Payment

Process a payment transaction.

**Endpoint**: `POST /payments`

**Authentication**: Required

**Request Body**:
```json
{
  "card_number": "4111111111111111",
  "cvv": "123",
  "expiry": "12/25",
  "amount": 99.99,
  "currency": "USD",
  "merchant_id": "MERCHANT_001",
  "customer_id": "CUST_12345"
}
```

**Field Descriptions**:
- `card_number` (string, required): Full card number (13-19 digits)
- `cvv` (string, required): Card verification value
- `expiry` (string, required): Expiration date (MM/YY format)
- `amount` (number, required): Transaction amount
- `currency` (string, optional): Currency code (default: USD)
- `merchant_id` (string, optional): Merchant identifier (default: DEFAULT)
- `customer_id` (string, optional): Customer identifier (default: ANONYMOUS)

**Card Types Supported**:
- Visa (starts with 4)
- Mastercard (starts with 5)
- American Express (starts with 3)

**Request Example**:
```bash
curl -X POST http://localhost:8080/payments \
  -H "X-API-Key: test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "cvv": "123",
    "expiry": "12/25",
    "amount": 99.99,
    "currency": "USD",
    "merchant_id": "MERCHANT_001",
    "customer_id": "CUST_12345"
  }'
```

**Response (Success - 200)**:
```json
{
  "transaction_id": "TXN20241225143022",
  "status": 1,
  "card_number": "4111111111111111",
  "amount": 99.99
}
```

**Status Codes**:
- `0` - Pending
- `1` - Approved
- `2` - Declined
- `3` - Error
- `4` - Fraud
- `99` - Unknown

⚠️ **Security Issue**: Response echoes back the full card number!

**Response (Error - 500)**:
```json
{
  "error": true,
  "code": 500,
  "message": "Error: [detailed stack trace exposed]"
}
```

### 3. Get Transaction

Retrieve details for a specific transaction.

**Endpoint**: `GET /transactions/{transaction_id}`

**Authentication**: Required

**Request**:
```bash
curl -X GET http://localhost:8080/transactions/TXN20241225143022 \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "id": "TXN20241225143022",
  "status": "fetched",
  "warning": "This would return full card details in production!"
}
```

⚠️ **Note**: Currently returns mock data. See issue #4 to implement actual database lookup.

### 4. Get All Transactions

Retrieve a list of transactions with optional search and limit.

**Endpoint**: `GET /transactions`

**Authentication**: Required

**Query Parameters**:
- `limit` (number, optional): Maximum number of results (default: 1000)
- `search` (string, optional): Search term (⚠️ SQL injection vulnerable!)

**Request**:
```bash
curl -X GET "http://localhost:8080/transactions?limit=10&search=CUST_123" \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "transactions": [],
  "limit": 10,
  "search": "CUST_123"
}
```

⚠️ **Security Issue**: The `search` parameter is vulnerable to SQL injection!

### 5. Get Customer

Retrieve customer details by ID.

**Endpoint**: `GET /customers/{customer_id}`

**Authentication**: Required

**Request**:
```bash
curl -X GET http://localhost:8080/customers/CUST_12345 \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "id": "CUST_12345",
  "email": "customer@example.com",
  "ssn": "123-45-6789",
  "card_number": "4111111111111111",
  "password": "plain_text_password"
}
```

⚠️ **Security Issues**:
- Returns SSN in plain text
- Returns full card number
- Returns password in plain text
- No authorization check (anyone can view any customer!)

### 6. Create Customer

Create a new customer account.

**Endpoint**: `POST /customers`

**Authentication**: Required

**Request Body**:
```json
{
  "email": "newcustomer@example.com",
  "first_name": "John",
  "last_name": "Doe",
  "password": "secret123"
}
```

**Request**:
```bash
curl -X POST http://localhost:8080/customers \
  -H "X-API-Key: test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newcustomer@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "password": "secret123"
  }'
```

**Response**:
```json
{
  "success": true,
  "customer_id": "CUST_1640438400000",
  "password": "secret123"
}
```

⚠️ **Security Issue**: Echoes password back in response!

### 7. Process Refund

Refund a transaction.

**Endpoint**: `POST /refunds`

**Authentication**: Required

**Request Body**:
```json
{
  "transaction_id": "TXN20241225143022",
  "reason": "Customer request"
}
```

**Request**:
```bash
curl -X POST http://localhost:8080/refunds \
  -H "X-API-Key: test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "TXN20241225143022",
    "reason": "Customer request"
  }'
```

**Response**:
```json
{
  "success": true,
  "transaction_id": "TXN20241225143022"
}
```

⚠️ **Security Issue**: No authorization check - anyone can refund any transaction!

### 8. Health Check

Check if the API is operational.

**Endpoint**: `GET /health`

**Authentication**: Required

**Request**:
```bash
curl -X GET http://localhost:8080/health \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "status": "healthy",
  "timestamp": 1640438400000,
  "database": "connected",
  "memory_used": 134217728,
  "uptime": "TODO"
}
```

### 9. Debug Info (⚠️ DANGEROUS)

**Endpoint**: `GET /debug`

**Authentication**: Required

⚠️ **CRITICAL SECURITY ISSUE**: This endpoint exposes all secrets!

**Request**:
```bash
curl -X GET http://localhost:8080/debug \
  -H "X-API-Key: test_key_12345"
```

**Response**:
```json
{
  "api_keys": ["test_key_12345", "prod_key_67890", "internal_key_admin", "backdoor_key_debug123"],
  "system_properties": "{ ... all system properties ... }",
  "environment": "{ ... all environment variables ... }",
  "statistics": {
    "transactionCount": 12345,
    "cacheSize": 100,
    "dbUrl": "jdbc:mysql://prod-db.megabank.internal:3306/payments",
    "dbUser": "payment_admin",
    "dbPassword": "Payment123!",
    "visaApiKey": "vk_live_a7b3c9d2e5f1g8h4"
  }
}
```

**This endpoint should NEVER exist in production!**

## Error Responses

All endpoints may return error responses in this format:

```json
{
  "error": true,
  "code": 400,
  "message": "Error description (may include sensitive details)"
}
```

**HTTP Status Codes**:
- `200` - Success
- `401` - Invalid API key
- `404` - Endpoint not found
- `500` - Internal server error (with stack trace!)

## Rate Limiting

⚠️ **There is no rate limiting!** You can make unlimited requests.

Configuration in `application.properties`:
```properties
rate.limit.enabled=false
rate.limit.requests=1000000
rate.limit.period=1
```

## CORS Configuration

⚠️ **CORS is set to `*`** - any origin can access the API!

```java
response.setHeader("Access-Control-Allow-Origin", "*");
```

## Security Considerations

### Known Vulnerabilities

1. **SQL Injection**
   - `/transactions` search parameter
   - All customer operations
   - See issue #7 for remediation

2. **Sensitive Data Exposure**
   - Full card numbers in responses
   - CVV returned in some responses
   - Passwords in plain text
   - SSN exposure
   - `/debug` endpoint

3. **Authentication & Authorization**
   - Hardcoded API keys
   - No authorization checks
   - Anyone can refund any transaction
   - Anyone can view any customer

4. **PCI-DSS Violations**
   - CVV storage (requirement 3.2 violation)
   - Full card number storage
   - See issue #9 for CVV removal plan

5. **Information Disclosure**
   - Stack traces in error responses
   - Debug endpoint exposes all secrets
   - Excessive logging of sensitive data

### Recommendations

For a production system, you should:
- Use HTTPS exclusively
- Implement proper authentication (OAuth 2.0, JWT)
- Add authorization checks on all endpoints
- Never store CVV
- Tokenize card numbers
- Use PreparedStatements for SQL
- Implement rate limiting
- Remove debug endpoints
- Sanitize error messages
- Enable CSRF protection
- Restrict CORS to specific origins

## Testing the API

### Using cURL

See examples above for each endpoint.

### Using Postman

Import this collection (create a `postman_collection.json`):

```json
{
  "info": {
    "name": "MegaBank Payment Gateway",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Process Payment",
      "request": {
        "method": "POST",
        "header": [
          {"key": "X-API-Key", "value": "test_key_12345"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "url": "http://localhost:8080/payments",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"card_number\": \"4111111111111111\",\n  \"cvv\": \"123\",\n  \"expiry\": \"12/25\",\n  \"amount\": 99.99\n}"
        }
      }
    }
  ]
}
```

### Test Card Numbers

Use these test card numbers (they're validated but not charged):

- **Visa**: `4111111111111111`
- **Mastercard**: `5555555555554444`
- **Amex**: `378282246310005`

**Easter Egg**: Cards ending in `42` are always approved! 🎉

## Code Examples

### Java

```java
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

public class PaymentExample {
    public static void main(String[] args) throws Exception {
        URL url = new URL("http://localhost:8080/payments");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-API-Key", "test_key_12345");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\"card_number\":\"4111111111111111\",\"cvv\":\"123\",\"expiry\":\"12/25\",\"amount\":99.99}";

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);
    }
}
```

### Python

```python
import requests
import json

url = "http://localhost:8080/payments"
headers = {
    "X-API-Key": "test_key_12345",
    "Content-Type": "application/json"
}
data = {
    "card_number": "4111111111111111",
    "cvv": "123",
    "expiry": "12/25",
    "amount": 99.99
}

response = requests.post(url, headers=headers, json=data)
print(f"Status: {response.status_code}")
print(f"Response: {response.json()}")
```

### JavaScript (Node.js)

```javascript
const axios = require('axios');

const payment = {
  card_number: '4111111111111111',
  cvv: '123',
  expiry: '12/25',
  amount: 99.99
};

axios.post('http://localhost:8080/payments', payment, {
  headers: {
    'X-API-Key': 'test_key_12345',
    'Content-Type': 'application/json'
  }
})
.then(response => {
  console.log('Transaction ID:', response.data.transaction_id);
  console.log('Status:', response.data.status);
})
.catch(error => {
  console.error('Error:', error.response.data);
});
```

## OpenAPI/Swagger Specification

For a machine-readable API specification, see the OpenAPI 3.0 spec (coming soon - see issue for contribution opportunity!).

## Support

- **Issues**: [GitHub Issues](https://github.com/pmanlukas/Payment-Gateway/issues)
- **Documentation**: [README.md](README.md)
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md)

---

**Remember**: This API is intentionally insecure for demonstration purposes. Never build a real payment API like this!
