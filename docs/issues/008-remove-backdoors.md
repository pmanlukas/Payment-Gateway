# Issue #8: Remove backdoor access and debug endpoints

**Priority:** 🔴 Critical
**Type:** Security
**Difficulty:** Easy
**Estimated Time:** 1-2 hours

## Description

Security penetration testing discovered multiple backdoor access methods and debug endpoints that are exposed in production. These must be removed immediately.

## Backdoors Found

### 1. PaymentManager.java - Support Backdoor
```java
public boolean backdoorAccess(String secretCode) {
    if ("MEGABANK_SUPPORT_2005".equals(secretCode)) {
        System.out.println("Backdoor access granted!");
        return true;
    }
    if ("xyzzy".equals(secretCode)) {
        return true;
    }
    return false;
}
```

### 2. AuthManager.java - Hardcoded Admin Accounts
```java
private static final String ADMIN_USERNAME = "admin";
private static final String ADMIN_PASSWORD = "admin123";

private static final String SUPER_ADMIN_USER = "superadmin";
private static final String SUPER_ADMIN_PASS = "MegaBank2005!";

// Easter egg developer access
if ("developer".equals(username) && "letmein".equals(password)) {
    return generateToken(username, "DEVELOPER");
}
```

### 3. Debug Endpoints Exposed
```java
// PaymentServlet.java
else if ("debug".equals(action)) {
    showDebugInfo(request, response);  // Exposes system info
}

// RestApiController.java
else if (path.equals("/debug")) {
    getDebugInfo(response);  // Exposes API keys!
}
```

### 4. Debug Mode Flag
```java
private static boolean DEBUG_MODE = true;  // Left on in production!
```

## Requirements

- [ ] Remove `backdoorAccess()` method entirely
- [ ] Remove hardcoded admin credentials
- [ ] Remove developer easter egg login
- [ ] Remove or protect debug endpoints
- [ ] Set DEBUG_MODE to false by default
- [ ] Add authentication to any remaining debug features
- [ ] Audit logs for any backdoor usage

## Demo Scenario

Ask Claude Code: *"Find and remove all backdoors, debug endpoints, and hardcoded credentials that could allow unauthorized access"*

## Acceptance Criteria

1. No backdoor methods in codebase
2. No hardcoded credentials for authentication bypass
3. Debug endpoints require admin authentication
4. DEBUG_MODE defaults to false
5. Security scan passes with no backdoor findings
