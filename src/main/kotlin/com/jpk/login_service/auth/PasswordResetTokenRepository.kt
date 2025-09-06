package com.jpk.login_service.auth

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email: String): PasswordResetToken?
    fun deleteAllByExpiresAtBefore(now: Instant): Long
}
