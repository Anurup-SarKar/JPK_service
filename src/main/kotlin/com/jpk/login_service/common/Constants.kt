package com.jpk.login_service.common

// Compile-time constants for reuse (usable in annotations)
const val PASSWORD_POLICY_MESSAGE: String =
        "Password must be at least 8 chars and include upper, lower, digit, special"

object Messages {
    // Auth
    const val INVALID_CREDENTIALS = "Invalid credentials"
    const val OTP_NOT_FOUND = "OTP not found"
    const val OTP_EXPIRED = "OTP expired"
    const val OTP_INVALID = "Invalid OTP"
    const val OTP_SENT = "OTP sent"
    const val OTP_VALIDATED = "OTP validated"
    const val OTP_RESENT = "OTP re-sent"

    // User management
    const val USER_CREATED = "User created"
    const val USER_UPDATED = "User updated"
    const val USER_DELETED = "User deleted"
    const val USER_NOT_FOUND = "User not found"
    // Removed generic for duplicates in favor of specific ones (kept for fallback)
    const val USER_ALREADY_EXISTS = "User already exists"
    const val USER_LIST = "User list"
    const val USERNAME_ALREADY_EXISTS = "Username already exists"
    const val EMAIL_ALREADY_EXISTS = "Email already exists"
    const val MOBILE_ALREADY_EXISTS = "Mobile number already exists"

    // Validation / Errors
    const val VALIDATION_FAILED = "Validation failed"
    const val BAD_REQUEST = "Bad request"
    const val INTERNAL_ERROR = "Internal server error"

    // Mail
    const val MAIL_OTP_SUBJECT = "Your Login OTP"
    const val MAIL_OTP_BODY_TEMPLATE = "Your OTP is: %s. It expires in %d minutes." // otp, minutes

    // Config values
    const val OTP_TTL_MINUTES = 5L
}
