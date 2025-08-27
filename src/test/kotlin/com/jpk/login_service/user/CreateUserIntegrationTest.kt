package com.jpk.login_service.user

import com.jpk.login_service.common.PasswordUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateUserIntegrationTest {

    @Test
    fun testCreateUserRequestWithValidSHA256() {
        // Generate a proper SHA-256 hash
        val rawPassword = "TestPassword123!"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)

        // Create a valid request
        val request =
                CreateUserRequest(
                        username = "testuser",
                        email = "test@example.com",
                        mobile = "1234567890",
                        passwordHash = sha256Hash,
                        fullName = "Test User",
                        cctvLink = null,
                        isCctvVisible = false,
                        isCctvStorageVisible = false,
                        isAdmin = false,
                        isActive = true
                )

        // Verify SHA-256 hash format
        assertEquals(64, request.passwordHash.length)
        assertTrue(request.passwordHash.matches(Regex("^[a-fA-F0-9]{64}$")))

        // Test that PasswordUtils can hash this
        val bcryptHash = PasswordUtils.hashPassword(request.passwordHash)
        assertTrue(PasswordUtils.isBCryptHash(bcryptHash))
        assertTrue(PasswordUtils.verifyPassword(request.passwordHash, bcryptHash))
    }

    @Test
    fun testInvalidPasswordHashFormats() {
        val validData = mapOf("username" to "testuser", "email" to "test@example.com")

        val invalidHashes =
                listOf(
                        "tooshort", // Too short
                        "12345678901234567890123456789012345678901234567890123456789012345", // Too
                        // long
                        // (65
                        // chars)
                        "1234567890123456789012345678901234567890123456789012345678901234zz", // Invalid chars
                        "", // Empty
                        "PlainPassword123!" // Not hex
                )

        invalidHashes.forEach { invalidHash ->
            assertThrows<IllegalArgumentException> { PasswordUtils.hashPassword(invalidHash) }
        }
    }
}
