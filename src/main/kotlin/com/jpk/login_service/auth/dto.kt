package com.jpk.login_service.auth

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

// Login request - password should be SHA-256 hash from frontend
data class LoginRequest(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank
        @field:Pattern(
                regexp = "^[a-fA-F0-9]{64}$",
                message = "Password must be SHA-256 hash (64 hex characters)"
        )
        @JsonAlias("password")
        val passwordHash: String // accepts password or passwordHash
)

// Response after login (OTP only)
data class OtpResponse(val otp: String, val expiresInSeconds: Long)

// OTP validation request - password should be SHA-256 hash from frontend
data class OtpValidateRequest(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank
        @field:Pattern(
                regexp = "^[a-fA-F0-9]{64}$",
                message = "Password must be SHA-256 hash (64 hex characters)"
        )
        @JsonAlias("password")
        val passwordHash: String, // accepts password or passwordHash
        @field:NotBlank @field:Pattern(regexp = "\\d{6}") val otp: String
)

// User data returned after OTP validation
data class UserDataResponse(
        val id: Long?,
        val username: String,
        val email: String,
        val mobile: String?,
        val fullName: String?,
        val cctvLink: String?,
        val isCctvVisible: Boolean,
        val isCctvStorageVisible: Boolean,
        val isActive: Boolean
)
