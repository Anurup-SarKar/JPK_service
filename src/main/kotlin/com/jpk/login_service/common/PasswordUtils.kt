package com.jpk.login_service.common

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest

/**
 * Double-protected password utility:
 * 1. Frontend sends SHA-256 hash of raw password
 * 2. Backend applies BCrypt on the received hash
 * 
 * This ensures:
 * - Raw passwords never travel over network
 * - Even if SHA-256 hash is intercepted, it's still protected by BCrypt
 * - BCrypt provides salt and makes brute force attacks extremely difficult
 */
object PasswordUtils {
    
    private const val BCRYPT_ROUNDS = 12
    
    /**
     * Hash a SHA-256 password hash (received from frontend) using BCrypt
     * @param sha256Hash The SHA-256 hash of the raw password from frontend
     * @return BCrypt hash of the SHA-256 hash
     */
    fun hashPassword(sha256Hash: String): String {
        validateSha256Hash(sha256Hash)
        return BCrypt.hashpw(sha256Hash, BCrypt.gensalt(BCRYPT_ROUNDS))
    }
    
    /**
     * Verify a SHA-256 password hash against a stored BCrypt hash
     * @param sha256Hash The SHA-256 hash from frontend to verify
     * @param storedBCryptHash The stored BCrypt hash from database
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(sha256Hash: String, storedBCryptHash: String): Boolean {
        return try {
            validateSha256Hash(sha256Hash)
            BCrypt.checkpw(sha256Hash, storedBCryptHash)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate SHA-256 hash (for reference/testing - should be done by frontend)
     * @param rawPassword The raw password
     * @return SHA-256 hash in hex format
     */
    fun generateSha256Hash(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawPassword.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if a password hash is already BCrypt format
     * @param hash The hash to check
     * @return true if it's BCrypt format, false otherwise
     */
    fun isBCryptHash(hash: String): Boolean {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")
    }
    
    /**
     * Validate that the input is a proper SHA-256 hash
     * @param hash The hash to validate
     * @throws IllegalArgumentException if not a valid SHA-256 hash
     */
    private fun validateSha256Hash(hash: String) {
        if (hash.length != 64) {
            throw IllegalArgumentException("SHA-256 hash must be 64 characters long")
        }
        if (!hash.matches(Regex("^[a-fA-F0-9]+$"))) {
            throw IllegalArgumentException("SHA-256 hash must contain only hexadecimal characters")
        }
    }
    
    /**
     * Migrate old password hash to new format
     * This handles migration from old single-hash system to new double-protection system
     * @param oldHash The old hash format
     * @param rawPassword The raw password (if available during migration)
     * @return New BCrypt hash
     */
    fun migrateOldHash(oldHash: String, rawPassword: String? = null): String {
        return when {
            // If it's already BCrypt, return as is
            isBCryptHash(oldHash) -> oldHash
            
            // If we have the raw password, generate proper double hash
            rawPassword != null -> {
                val sha256Hash = generateSha256Hash(rawPassword)
                hashPassword(sha256Hash)
            }
            
            // Legacy: treat old hash as if it was SHA-256 (not ideal but handles migration)
            oldHash.length == 64 && oldHash.matches(Regex("^[a-fA-F0-9]+$")) -> {
                hashPassword(oldHash)
            }
            
            // For other formats, we need the raw password to migrate properly
            else -> throw IllegalArgumentException("Cannot migrate hash without raw password")
        }
    }
}
