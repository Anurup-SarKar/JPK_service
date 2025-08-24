package com.jpk.login_service.auth

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_otps")
class OtpEntry(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
        @Column(nullable = false, length = 50) var username: String,
        @Column(nullable = false, length = 6) var otp: String,
        @Column(name = "expires_at", nullable = false) var expiresAt: Instant,
        @Column(name = "consumed", nullable = false) var consumed: Boolean = false,
        @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)
