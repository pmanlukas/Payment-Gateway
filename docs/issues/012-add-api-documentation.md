# Issue #12: Add API documentation with OpenAPI/Swagger

**Priority:** 🟢 Low
**Type:** Documentation
**Difficulty:** Easy
**Estimated Time:** 3-4 hours

## Description

The REST API has no documentation. Developers integrating with our payment gateway have no way to understand the API without reading source code.

## Current State

The README says:
> ### Authentication
> Use an API key. Ask Bob for one (Bob left in 2007).

This is not helpful.

## API Endpoints (Undocumented)

### RestApiController.java

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/ | API info |
| GET | /api/v1/transactions | List transactions |
| GET | /api/v1/transactions/{id} | Get transaction |
| GET | /api/v1/customers/{id} | Get customer |
| POST | /api/v1/payments | Process payment |
| POST | /api/v1/refunds | Process refund |
| POST | /api/v1/customers | Create customer |
| GET | /api/v1/health | Health check |

### PaymentServlet.java (Legacy)

| Method | Path | Description |
|--------|------|-------------|
| GET | /payment?action=status | Transaction status |
| POST | /payment?action=process | Process payment |
| POST | /payment?action=refund | Refund |
| POST | /payment?action=batch | Batch processing |

## Requirements

- [ ] Add OpenAPI 3.0 specification file
- [ ] Document all endpoints with request/response schemas
- [ ] Add example requests and responses
- [ ] Document authentication mechanism
- [ ] Document error codes and messages
- [ ] Add rate limiting documentation
- [ ] Set up Swagger UI for interactive docs
- [ ] Generate client SDKs from spec

## Demo Scenario

Ask Claude Code: *"Generate OpenAPI documentation for our REST API based on the RestApiController class"*

## Documentation Template

```yaml
openapi: 3.0.0
info:
  title: MegaBank Payment Gateway API
  version: 1.0.0
paths:
  /payments:
    post:
      summary: Process a payment
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentRequest'
      responses:
        200:
          description: Payment processed
```

## Acceptance Criteria

1. OpenAPI spec file in repository
2. All endpoints documented
3. Request/response schemas defined
4. Swagger UI accessible
5. Examples for all operations
