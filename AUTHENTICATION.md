# Double-Protection Authentication System

## Overview

This authentication system implements double-layer password protection:

1. **Frontend**: Generates SHA-256 hash of raw password
2. **Backend**: Applies BCrypt hashing to the received SHA-256 hash

## Benefits

- Raw passwords never travel over the network
- Even if SHA-256 hash is intercepted, it's protected by BCrypt
- BCrypt provides salt and resistance against rainbow table attacks
- Future-proof (easy to increase BCrypt rounds)

## Frontend Implementation

### JavaScript/TypeScript Example

```javascript
async function hashPassword(rawPassword) {
    const encoder = new TextEncoder();
    const data = encoder.encode(rawPassword);
    const hash = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hash));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// Usage in registration
async function registerUser(email, username, rawPassword) {
    const passwordHash = await hashPassword(rawPassword);
    
    const response = await fetch('/api/users', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            username,
            email,
            passwordHash, // Send SHA-256 hash, not raw password
            // ... other fields
        })
    });
    
    return response.json();
}

// Usage in login
async function loginUser(email, rawPassword) {
    const passwordHash = await hashPassword(rawPassword);
    
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email,
            passwordHash // Send SHA-256 hash, not raw password
        })
    });
    
    return response.json();
}
```

### React Hook Example

```javascript
import { useState, useCallback } from 'react';

export function usePasswordHash() {
    const [isHashing, setIsHashing] = useState(false);
    
    const hashPassword = useCallback(async (rawPassword) => {
        setIsHashing(true);
        try {
            const encoder = new TextEncoder();
            const data = encoder.encode(rawPassword);
            const hash = await crypto.subtle.digest('SHA-256', data);
            const hashArray = Array.from(new Uint8Array(hash));
            return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        } finally {
            setIsHashing(false);
        }
    }, []);
    
    return { hashPassword, isHashing };
}
```

## API Changes

### Registration Endpoint: `POST /api/users`

**OLD Request Body:**
```json
{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "MyPassword123!",  // Raw password
    "fullName": "John Doe"
}
```

**NEW Request Body:**
```json
{
    "username": "john_doe",
    "email": "john@example.com",
    "passwordHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",  // SHA-256 hash
    "fullName": "John Doe"
}
```

### Login Endpoint: `POST /api/auth/login`

**OLD Request Body:**
```json
{
    "email": "john@example.com",
    "password": "MyPassword123!"  // Raw password
}
```

**NEW Request Body:**
```json
{
    "email": "john@example.com",
    "passwordHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"  // SHA-256 hash
}
```

### OTP Validation Endpoint: `POST /api/auth/validate-otp`

**OLD Request Body:**
```json
{
    "email": "john@example.com",
    "password": "MyPassword123!",  // Raw password
    "otp": "123456"
}
```

**NEW Request Body:**
```json
{
    "email": "john@example.com",
    "passwordHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",  // SHA-256 hash
    "otp": "123456"
}
```

## Password Hash Format

- **Length**: Exactly 64 characters
- **Characters**: Only hexadecimal (0-9, a-f, A-F)
- **Example**: `a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3`

## Validation

The backend validates that `passwordHash` fields:
- Are exactly 64 characters long
- Contain only hexadecimal characters
- Are not empty or null

Invalid hashes will return HTTP 400 with validation error messages.

## Migration

For existing users with old password formats:

1. Use the admin endpoint `POST /api/admin/migrate-passwords` to attempt automatic migration
2. For proper migration, use `POST /api/admin/migrate-user-password` with the user's raw password
3. Check migration status with `GET /api/admin/password-migration-status`

## Security Notes

1. **HTTPS Required**: Always use HTTPS in production to protect SHA-256 hashes in transit
2. **No Logging**: Never log password hashes or raw passwords
3. **Rate Limiting**: Implement rate limiting on login attempts
4. **Client-Side Storage**: Never store raw passwords on the client side
5. **Memory Clearing**: Clear password variables from memory after hashing

## Testing

Use the provided test utilities:

```kotlin
// In tests, you can generate SHA-256 hashes for testing
val testPasswordHash = PasswordUtils.generateSha256Hash("TestPassword123!")

// Verify the complete flow
val bcryptHash = PasswordUtils.hashPassword(testPasswordHash)
assertTrue(PasswordUtils.verifyPassword(testPasswordHash, bcryptHash))
```

## Error Handling

Frontend should handle these validation errors:

- `400 Bad Request`: Invalid password hash format
- `401 Unauthorized`: Invalid credentials (wrong password)
- `422 Unprocessable Entity`: Validation failed

Example error response:
```json
{
    "statusCode": 400,
    "statusMessage": "Validation failed",
    "data": {
        "passwordHash": ["Password must be SHA-256 hash (64 hex characters)"]
    }
}
```
