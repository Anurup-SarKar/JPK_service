package com.jpk.login_service.user

import com.jpk.login_service.common.PasswordUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PasswordMigrationServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordMigrationService: PasswordMigrationService

    @BeforeEach
    fun setup() {
        userRepository = mock()

        // Mock userRepository.save to return the argument
        whenever(userRepository.save(any<User>())).thenAnswer { invocation ->
            invocation.getArgument<User>(0)
        }

        passwordMigrationService = PasswordMigrationService(userRepository)
    }

    @Test
    fun `migrateAllPasswords should skip already migrated users`() {
        val bcryptUser =
                User(
                        id = 1L,
                        username = "bcryptuser",
                        email = "bcrypt@example.com",
                        passwordHash = "\$2a\$10\$abcdefghijklmnopqrstuvwxyz1234567890",
                        fullName = "BCrypt User"
                )

        whenever(userRepository.findAll()).thenReturn(listOf(bcryptUser))

        val result = passwordMigrationService.migrateAllPasswords()

        assertEquals(0, result.migratedCount)
        assertEquals(1, result.skippedCount)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `migrateAllPasswords should migrate non-BCrypt passwords`() {
        val oldUser =
                User(
                        id = 1L,
                        username = "olduser",
                        email = "old@example.com",
                        passwordHash =
                                "a" +
                                        "0123456789abcdef".repeat(3) +
                                        "0123456789abcde", // 64-char hex
                        fullName = "Old User"
                )

        whenever(userRepository.findAll()).thenReturn(listOf(oldUser))

        val result = passwordMigrationService.migrateAllPasswords()

        assertEquals(1, result.migratedCount)
        assertEquals(0, result.skippedCount)
        verify(userRepository).save(any())
        assertTrue(PasswordUtils.isBCryptHash(oldUser.passwordHash))
    }

    @Test
    fun `migrateAllPasswords should handle mix of migrated and non-migrated users`() {
        val bcryptUser =
                User(
                        id = 1L,
                        username = "bcryptuser",
                        email = "bcrypt@example.com",
                        passwordHash = "\$2a\$10\$abcdefghijklmnopqrstuvwxyz1234567890",
                        fullName = "BCrypt User"
                )

        val oldUser =
                User(
                        id = 2L,
                        username = "olduser",
                        email = "old@example.com",
                        passwordHash =
                                "a" +
                                        "0123456789abcdef".repeat(3) +
                                        "0123456789abcde", // 64-char hex
                        fullName = "Old User"
                )

        whenever(userRepository.findAll()).thenReturn(listOf(bcryptUser, oldUser))

        val result = passwordMigrationService.migrateAllPasswords()

        assertEquals(1, result.migratedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        verify(userRepository, times(1)).save(any())
    }

    @Test
    fun `getUnmigratedPasswordCount should count non-BCrypt passwords`() {
        val bcryptUser =
                User(
                        id = 1L,
                        username = "bcryptuser",
                        email = "bcrypt@example.com",
                        passwordHash = "\$2a\$10\$abcdefghijklmnopqrstuvwxyz1234567890",
                        fullName = "BCrypt User"
                )

        val oldUser1 =
                User(
                        id = 2L,
                        username = "olduser1",
                        email = "old1@example.com",
                        passwordHash = "plainsha256hash1",
                        fullName = "Old User1"
                )

        val oldUser2 =
                User(
                        id = 3L,
                        username = "olduser2",
                        email = "old2@example.com",
                        passwordHash = "plainsha256hash2",
                        fullName = "Old User2"
                )

        whenever(userRepository.findAll()).thenReturn(listOf(bcryptUser, oldUser1, oldUser2))

        val count = passwordMigrationService.getUnmigratedPasswordCount()

        assertEquals(2, count)
    }

    @Test
    fun `migrateUserPassword should migrate specific user with raw password`() {
        val userId = 1L
        val rawPassword = "MySecurePassword123"
        val user =
                User(
                        id = userId,
                        username = "testuser",
                        email = "user@example.com",
                        passwordHash = "oldHash",
                        fullName = "Test User"
                )

        whenever(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user))

        val success = passwordMigrationService.migrateUserPassword(userId, rawPassword)

        assertTrue(success)
        assertTrue(PasswordUtils.isBCryptHash(user.passwordHash))
        verify(userRepository).save(user)
    }

    @Test
    fun `migrateUserPassword should return false for non-existent user`() {
        val userId = 999L
        val rawPassword = "password"

        whenever(userRepository.findById(userId)).thenReturn(java.util.Optional.empty())

        val success = passwordMigrationService.migrateUserPassword(userId, rawPassword)

        assertFalse(success)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `migrateUserPassword should properly hash password with SHA-256 then BCrypt`() {
        val userId = 1L
        val rawPassword = "TestPassword123"
        val user =
                User(
                        id = userId,
                        username = "testuser",
                        email = "user@example.com",
                        passwordHash = "oldHash",
                        fullName = "Test User"
                )

        whenever(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user))

        passwordMigrationService.migrateUserPassword(userId, rawPassword)

        // Verify the password can be validated correctly
        val sha256Hash = PasswordUtils.sha256Hex(rawPassword)
        assertTrue(PasswordUtils.verifyPassword(sha256Hash, user.passwordHash))
    }
}
