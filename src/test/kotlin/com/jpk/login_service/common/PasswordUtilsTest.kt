package com.jpk.login_service.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class PasswordUtilsTest {

    @Test
    fun testSha256HashGeneration() {
        val password = "TestPassword123!"
        val sha256Hash = PasswordUtils.generateSha256Hash(password)
        
        // SHA-256 hash should be 64 hex characters
        assertEquals(64, sha256Hash.length)
        assertTrue(sha256Hash.matches(Regex("^[a-fA-F0-9]+$")))
        
        // Same password should generate same hash
        val sha256Hash2 = PasswordUtils.generateSha256Hash(password)
        assertEquals(sha256Hash, sha256Hash2)
    }

    @Test
    fun testBCryptPasswordHashing() {
        val rawPassword = "TestPassword123!"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        
        // Hash the SHA-256 with BCrypt
        val bcryptHash = PasswordUtils.hashPassword(sha256Hash)
        
        // BCrypt hash should start with $2a$, $2b$, or $2y$
        assertTrue(PasswordUtils.isBCryptHash(bcryptHash))
        
        // Verify the password correctly
        assertTrue(PasswordUtils.verifyPassword(sha256Hash, bcryptHash))
        
        // Wrong SHA-256 hash should fail
        val wrongSha256 = PasswordUtils.generateSha256Hash("WrongPassword")
        assertFalse(PasswordUtils.verifyPassword(wrongSha256, bcryptHash))
    }

    @Test
    fun testDifferentBCryptHashesForSameSha256() {
        val sha256Hash = PasswordUtils.generateSha256Hash("TestPassword123!")
        
        val bcryptHash1 = PasswordUtils.hashPassword(sha256Hash)
        val bcryptHash2 = PasswordUtils.hashPassword(sha256Hash)
        
        // BCrypt hashes should be different due to different salts
        assertNotEquals(bcryptHash1, bcryptHash2)
        
        // But both should verify the same SHA-256 hash
        assertTrue(PasswordUtils.verifyPassword(sha256Hash, bcryptHash1))
        assertTrue(PasswordUtils.verifyPassword(sha256Hash, bcryptHash2))
    }

    @Test
    fun testInvalidSha256HashValidation() {
        // Test invalid length
        assertThrows<IllegalArgumentException> {
            PasswordUtils.hashPassword("tooshort")
        }
        
        // Test invalid characters
        assertThrows<IllegalArgumentException> {
            PasswordUtils.hashPassword("12345678901234567890123456789012345678901234567890123456789zzzzz")
        }
        
        // Test empty string
        assertThrows<IllegalArgumentException> {
            PasswordUtils.hashPassword("")
        }
    }

    @Test
    fun testBCryptHashDetection() {
        val validBCryptHashes = listOf(
            "\$2a\$12\$abcdefghijklmnopqrstuvwxyz123456789012345678901234567890",
            "\$2b\$10\$abcdefghijklmnopqrstuvwxyz123456789012345678901234567890",
            "\$2y\$08\$abcdefghijklmnopqrstuvwxyz123456789012345678901234567890"
        )
        
        validBCryptHashes.forEach { hash ->
            assertTrue(PasswordUtils.isBCryptHash(hash))
        }
        
        val invalidHashes = listOf(
            "plaintext",
            "12345678901234567890123456789012345678901234567890123456789012345",
            "\$1\$abc", // MD5 format
            "sha256:abcd1234"
        )
        
        invalidHashes.forEach { hash ->
            assertFalse(PasswordUtils.isBCryptHash(hash))
        }
    }

    @Test
    fun testMigrateOldHashWithRawPassword() {
        val rawPassword = "TestPassword123!"
        val oldPlaintextHash = "oldplaintexthash"
        
        // Migrate with raw password should work
        val migratedHash = PasswordUtils.migrateOldHash(oldPlaintextHash, rawPassword)
        assertTrue(PasswordUtils.isBCryptHash(migratedHash))
        
        // Should verify against proper SHA-256 of raw password
        val expectedSha256 = PasswordUtils.generateSha256Hash(rawPassword)
        assertTrue(PasswordUtils.verifyPassword(expectedSha256, migratedHash))
    }

    @Test
    fun testMigrateOldSha256Hash() {
        val rawPassword = "TestPassword123!"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        
        // Migrate old SHA-256 hash (treating it as if it was already SHA-256)
        val migratedHash = PasswordUtils.migrateOldHash(sha256Hash)
        assertTrue(PasswordUtils.isBCryptHash(migratedHash))
        
        // Should verify against the same SHA-256 hash
        assertTrue(PasswordUtils.verifyPassword(sha256Hash, migratedHash))
    }

    @Test
    fun testMigrateBCryptHashNoChange() {
        val sha256Hash = PasswordUtils.generateSha256Hash("TestPassword123!")
        val bcryptHash = PasswordUtils.hashPassword(sha256Hash)
        
        // Migrating BCrypt hash should return unchanged
        val result = PasswordUtils.migrateOldHash(bcryptHash)
        assertEquals(bcryptHash, result)
    }

    @Test
    fun testEndToEndDoubleProtection() {
        val rawPassword = "TestPassword123!"
        
        // 1. Frontend generates SHA-256 hash
        val frontendHash = PasswordUtils.generateSha256Hash(rawPassword)
        
        // 2. Backend receives SHA-256 hash and applies BCrypt
        val backendHash = PasswordUtils.hashPassword(frontendHash)
        
        // 3. Store backendHash in database
        assertTrue(PasswordUtils.isBCryptHash(backendHash))
        
        // 4. During login, verify the frontend SHA-256 against stored BCrypt
        assertTrue(PasswordUtils.verifyPassword(frontendHash, backendHash))
        
        // 5. Wrong password should fail at SHA-256 level
        val wrongFrontendHash = PasswordUtils.generateSha256Hash("WrongPassword")
        assertFalse(PasswordUtils.verifyPassword(wrongFrontendHash, backendHash))
    }

    @Test
    fun testVerifyPasswordWithInvalidInput() {
        val validSha256 = PasswordUtils.generateSha256Hash("TestPassword123!")
        val validBCrypt = PasswordUtils.hashPassword(validSha256)
        
        // Invalid SHA-256 should return false, not throw exception
        assertFalse(PasswordUtils.verifyPassword("invalid", validBCrypt))
        assertFalse(PasswordUtils.verifyPassword("", validBCrypt))
        assertFalse(PasswordUtils.verifyPassword("tooshort", validBCrypt))
        
        // Invalid BCrypt should return false
        assertFalse(PasswordUtils.verifyPassword(validSha256, "invalid"))
        assertFalse(PasswordUtils.verifyPassword(validSha256, ""))
    }
}
