# security-common

Shared internal library for JWT handling and auth-service token introspection.

## What it provides

- `SharedJwtProperties` (`security.jwt.*`)
- `AuthValidationProperties` (`integration.auth-service.*`)
- `BearerTokenService` for `Authorization: Bearer ...` extraction
- `JwtTokenService` for token generation and validation
- `AuthValidationClient` for `/users/token/validate` introspection
- `SharedSecurityConfiguration` to register all shared beans

## Service integration

Each microservice should:

1. Add dependency on `com.hfenelsoftllc:security-common:0.0.1-SNAPSHOT`
2. Import `SharedSecurityConfiguration` in an application config class
3. Keep service-specific exception mapping and business rules local

## Configuration

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:change-this-default-jwt-secret-key-change-this-default-jwt-secret-key}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}

integration:
  auth-service:
    base-url: ${AUTH_SERVICE_BASE_URL:http://localhost:9090}
```

