package com.jpk.login_service.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.jpk.login_service.common.GlobalExceptionHandler
import com.jpk.login_service.user.MigrationResult
import com.jpk.login_service.user.PasswordMigrationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var passwordMigrationService: PasswordMigrationService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migratePasswords should return migration result`() {
        val migrationResult = MigrationResult(
            totalUsers = 100,
            migratedCount = 95,
            skippedCount = 5,
            errorCount = 0,
            errors = emptyList()
        )

        whenever(passwordMigrationService.migrateAllPasswords()).thenReturn(migrationResult)

        mockMvc.perform(
            post("/api/admin/migrate-passwords")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value("Password migration completed"))
            .andExpect(jsonPath("$.data.totalUsers").value(100))
            .andExpect(jsonPath("$.data.migratedCount").value(95))
            .andExpect(jsonPath("$.data.skippedCount").value(5))
            .andExpect(jsonPath("$.data.errorCount").value(0))

        verify(passwordMigrationService).migrateAllPasswords()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migratePasswords should handle partial migration failures`() {
        val migrationResult = MigrationResult(
            totalUsers = 100,
            migratedCount = 80,
            skippedCount = 10,
            errorCount = 10,
            errors = listOf("User user1@example.com: Error", "User user2@example.com: Error")
        )

        whenever(passwordMigrationService.migrateAllPasswords()).thenReturn(migrationResult)

        mockMvc.perform(
            post("/api/admin/migrate-passwords")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.errorCount").value(10))
            .andExpect(jsonPath("$.data.migratedCount").value(80))

        verify(passwordMigrationService).migrateAllPasswords()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getPasswordMigrationStatus should return status when migration needed`() {
        val unmigratedCount = 50L

        whenever(passwordMigrationService.getUnmigratedPasswordCount()).thenReturn(unmigratedCount)

        mockMvc.perform(
            get("/api/admin/password-migration-status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value("Migration status retrieved"))
            .andExpect(jsonPath("$.data.unmigratedPasswordCount").value(50))
            .andExpect(jsonPath("$.data.needsMigration").value(true))
            .andExpect(jsonPath("$.data.migrationMessage").value(
                "Warning: 50 passwords need migration. Note: Proper migration requires raw passwords for SHA-256 generation."
            ))

        verify(passwordMigrationService).getUnmigratedPasswordCount()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getPasswordMigrationStatus should return status when no migration needed`() {
        val unmigratedCount = 0L

        whenever(passwordMigrationService.getUnmigratedPasswordCount()).thenReturn(unmigratedCount)

        mockMvc.perform(
            get("/api/admin/password-migration-status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.unmigratedPasswordCount").value(0))
            .andExpect(jsonPath("$.data.needsMigration").value(false))
            .andExpect(jsonPath("$.data.migrationMessage").value(
                "All passwords are using the new BCrypt format."
            ))

        verify(passwordMigrationService).getUnmigratedPasswordCount()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migrateUserPassword should migrate specific user successfully`() {
        val request = MigrateUserPasswordRequest(
            userId = 1L,
            rawPassword = "userRawPassword"
        )

        whenever(passwordMigrationService.migrateUserPassword(1L, "userRawPassword"))
            .thenReturn(true)

        mockMvc.perform(
            post("/api/admin/migrate-user-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value("User password migrated successfully"))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.migrated").value(true))

        verify(passwordMigrationService).migrateUserPassword(1L, "userRawPassword")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migrateUserPassword should return failure when migration fails`() {
        val request = MigrateUserPasswordRequest(
            userId = 999L,
            rawPassword = "userRawPassword"
        )

        whenever(passwordMigrationService.migrateUserPassword(999L, "userRawPassword"))
            .thenReturn(false)

        mockMvc.perform(
            post("/api/admin/migrate-user-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value("Failed to migrate user password"))
            .andExpect(jsonPath("$.data.userId").value(999))
            .andExpect(jsonPath("$.data.migrated").value(false))

        verify(passwordMigrationService).migrateUserPassword(999L, "userRawPassword")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migratePasswords should handle when all users already migrated`() {
        val migrationResult = MigrationResult(
            totalUsers = 100,
            migratedCount = 0,
            skippedCount = 100,
            errorCount = 0,
            errors = emptyList()
        )

        whenever(passwordMigrationService.migrateAllPasswords()).thenReturn(migrationResult)

        mockMvc.perform(
            post("/api/admin/migrate-passwords")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalUsers").value(100))
            .andExpect(jsonPath("$.data.migratedCount").value(0))
            .andExpect(jsonPath("$.data.skippedCount").value(100))

        verify(passwordMigrationService).migrateAllPasswords()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migratePasswords should handle empty database`() {
        val migrationResult = MigrationResult(
            totalUsers = 0,
            migratedCount = 0,
            skippedCount = 0,
            errorCount = 0,
            errors = emptyList()
        )

        whenever(passwordMigrationService.migrateAllPasswords()).thenReturn(migrationResult)

        mockMvc.perform(
            post("/api/admin/migrate-passwords")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalUsers").value(0))
            .andExpect(jsonPath("$.data.migratedCount").value(0))

        verify(passwordMigrationService).migrateAllPasswords()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getPasswordMigrationStatus should handle large unmigrated count`() {
        val unmigratedCount = 10000L

        whenever(passwordMigrationService.getUnmigratedPasswordCount()).thenReturn(unmigratedCount)

        mockMvc.perform(
            get("/api/admin/password-migration-status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.unmigratedPasswordCount").value(10000))
            .andExpect(jsonPath("$.data.needsMigration").value(true))

        verify(passwordMigrationService).getUnmigratedPasswordCount()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `migrateUserPassword should handle multiple user migrations in sequence`() {
        val request1 = MigrateUserPasswordRequest(userId = 1L, rawPassword = "password1")
        val request2 = MigrateUserPasswordRequest(userId = 2L, rawPassword = "password2")

        whenever(passwordMigrationService.migrateUserPassword(1L, "password1")).thenReturn(true)
        whenever(passwordMigrationService.migrateUserPassword(2L, "password2")).thenReturn(true)

        // Migrate first user
        mockMvc.perform(
            post("/api/admin/migrate-user-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.migrated").value(true))

        // Migrate second user
        mockMvc.perform(
            post("/api/admin/migrate-user-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(2))
            .andExpect(jsonPath("$.data.migrated").value(true))

        verify(passwordMigrationService).migrateUserPassword(1L, "password1")
        verify(passwordMigrationService).migrateUserPassword(2L, "password2")
    }
}
