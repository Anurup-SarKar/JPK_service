package com.jpk.login_service.user

import com.jpk.login_service.common.PasswordUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for migrating existing passwords to new double-protection system
 * This should be run once after deploying the new password system
 */
@Service
class PasswordMigrationService(
    private val userRepository: UserRepository
) {
    
    /**
     * Migrate passwords from old system to new BCrypt system
     * Note: This method requires manual intervention for proper migration
     * because we need the raw passwords to generate proper SHA-256 hashes
     */
    @Transactional
    fun migrateAllPasswords(): MigrationResult {
        val users = userRepository.findAll()
        var migratedCount = 0
        var skippedCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        
        users.forEach { user ->
            try {
                if (!PasswordUtils.isBCryptHash(user.passwordHash)) {
                    // Try to migrate using the old hash as SHA-256
                    val migratedHash = PasswordUtils.migrateOldHash(user.passwordHash)
                    user.passwordHash = migratedHash
                    userRepository.save(user)
                    migratedCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                errorCount++
                errors.add("User ${user.email}: ${e.message}")
            }
        }
        
        return MigrationResult(
            totalUsers = users.size,
            migratedCount = migratedCount,
            skippedCount = skippedCount,
            errorCount = errorCount,
            errors = errors
        )
    }
    
    /**
     * Check if a specific user's password is already in BCrypt format
     */
    fun isPasswordMigrated(userId: Long): Boolean {
        val user = userRepository.findById(userId).orElse(null)
        return user?.let { PasswordUtils.isBCryptHash(it.passwordHash) } ?: false
    }
    
    /**
     * Get count of users with old password format
     */
    fun getUnmigratedPasswordCount(): Long {
        val users = userRepository.findAll()
        return users.count { !PasswordUtils.isBCryptHash(it.passwordHash) }.toLong()
    }
    
    /**
     * Migrate a specific user's password (with raw password)
     * This should be used when the raw password is available
     */
    @Transactional
    fun migrateUserPassword(userId: Long, rawPassword: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        
        if (PasswordUtils.isBCryptHash(user.passwordHash)) {
            return true // Already migrated
        }
        
        try {
            val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
            user.passwordHash = PasswordUtils.hashPassword(sha256Hash)
            userRepository.save(user)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

data class MigrationResult(
    val totalUsers: Int,
    val migratedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val errors: List<String>
)
