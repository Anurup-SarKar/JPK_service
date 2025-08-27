package com.jpk.login_service.admin

import com.jpk.login_service.common.ApiResponse
import com.jpk.login_service.user.MigrationResult
import com.jpk.login_service.user.PasswordMigrationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(private val passwordMigrationService: PasswordMigrationService) {

    /**
     * Endpoint to migrate passwords from old format to new BCrypt format This should be called once
     * after deploying the new password system
     */
    @PostMapping("/migrate-passwords")
    fun migratePasswords(): ResponseEntity<ApiResponse<MigrationResult>> {
        val result = passwordMigrationService.migrateAllPasswords()
        return ResponseEntity.ok(
                ApiResponse(
                        statusCode = 200,
                        statusMessage = "Password migration completed",
                        data = result
                )
        )
    }

    /** Endpoint to check migration status */
    @GetMapping("/password-migration-status")
    fun getPasswordMigrationStatus(): ResponseEntity<ApiResponse<MigrationStatusResponse>> {
        val unmigratedCount = passwordMigrationService.getUnmigratedPasswordCount()
        return ResponseEntity.ok(
                ApiResponse(
                        statusCode = 200,
                        statusMessage = "Migration status retrieved",
                        data =
                                MigrationStatusResponse(
                                        unmigratedPasswordCount = unmigratedCount,
                                        needsMigration = unmigratedCount > 0,
                                        migrationMessage =
                                                if (unmigratedCount > 0) {
                                                    "Warning: $unmigratedCount passwords need migration. Note: Proper migration requires raw passwords for SHA-256 generation."
                                                } else {
                                                    "All passwords are using the new BCrypt format."
                                                }
                                )
                )
        )
    }

    /**
     * Endpoint to migrate a specific user's password with raw password This should be used when raw
     * password is available for proper migration
     */
    @PostMapping("/migrate-user-password")
    fun migrateUserPassword(
            @RequestBody request: MigrateUserPasswordRequest
    ): ResponseEntity<ApiResponse<UserMigrationResponse>> {
        val success =
                passwordMigrationService.migrateUserPassword(request.userId, request.rawPassword)

        return ResponseEntity.ok(
                ApiResponse(
                        statusCode = if (success) 200 else 400,
                        statusMessage =
                                if (success) "User password migrated successfully"
                                else "Failed to migrate user password",
                        data = UserMigrationResponse(userId = request.userId, migrated = success)
                )
        )
    }
}

data class MigrationStatusResponse(
        val unmigratedPasswordCount: Long,
        val needsMigration: Boolean,
        val migrationMessage: String
)

data class MigrateUserPasswordRequest(val userId: Long, val rawPassword: String)

data class UserMigrationResponse(val userId: Long, val migrated: Boolean)
