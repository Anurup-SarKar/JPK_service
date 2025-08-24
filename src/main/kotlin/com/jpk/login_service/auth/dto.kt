package com.jpk.login_service.auth

import com.jpk.login_service.common.PASSWORD_POLICY_MESSAGE
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

// Login request (username + password)
data class LoginRequest(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank
        @field:Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = PASSWORD_POLICY_MESSAGE
        )
        val password: String
)

// Response after login (OTP only)
data class OtpResponse(val otp: String, val expiresInSeconds: Long)

// OTP validation request
data class OtpValidateRequest(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank
        @field:Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = PASSWORD_POLICY_MESSAGE
        )
        val password: String,
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
