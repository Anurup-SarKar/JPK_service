package com.jpk.login_service.user

import com.jpk.login_service.common.PASSWORD_POLICY_MESSAGE
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

// Request DTO for creating a user
// (username kept for now; could be removed later if only email is used)

data class CreateUserRequest(
        @field:NotBlank val username: String,
        @field:Email @field:NotBlank val email: String,
        val mobile: String? = null,
        @field:NotBlank
        @field:Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = PASSWORD_POLICY_MESSAGE
        )
        val password: String,
        val fullName: String? = null,
        val cctvLink: String? = null,
        val isCctvVisible: Boolean = false,
        val isCctvStorageVisible: Boolean = false,
        val isAdmin: Boolean = false,
        val isActive: Boolean = true
)

// Request DTO for updating a user (all optional)

data class UpdateUserRequest(
        val username: String? = null,
        @field:Email val email: String? = null,
        val mobile: String? = null,
        @field:Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = PASSWORD_POLICY_MESSAGE
        )
        val password: String? = null,
        val fullName: String? = null,
        val cctvLink: String? = null,
        val isCctvVisible: Boolean? = null,
        val isCctvStorageVisible: Boolean? = null,
        val isAdmin: Boolean? = null,
        val isActive: Boolean? = null
)

// Request DTO for deleting a user by email

data class DeleteUserRequest(@field:Email @field:NotBlank val email: String)

// Response DTO for user

data class UserResponse(
        val id: Long?,
        val username: String,
        val email: String,
        val mobile: String?,
        val fullName: String?,
        val cctvLink: String?,
        val isCctvVisible: Boolean,
        val isCctvStorageVisible: Boolean,
        val isAdmin: Boolean,
        val isActive: Boolean
)

// Request DTO for updating a user by email (email is mandatory for identifying the user)

data class UpdateUserByEmailRequest(
        @field:Email @field:NotBlank val email: String,
        val username: String? = null,
        val mobile: String? = null,
        @field:Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = PASSWORD_POLICY_MESSAGE
        )
        val password: String? = null,
        val fullName: String? = null,
        val cctvLink: String? = null,
        val isCctvVisible: Boolean? = null,
        val isCctvStorageVisible: Boolean? = null,
        val isAdmin: Boolean? = null,
        val isActive: Boolean? = null
)
