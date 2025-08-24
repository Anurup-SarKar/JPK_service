package com.jpk.login_service.auth

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository

interface OtpRepository : JpaRepository<OtpEntry, Long> {
    fun findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(username: String): OtpEntry?
    fun deleteAllByExpiresAtBefore(now: Instant): Long
}
