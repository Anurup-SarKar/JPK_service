package com.jpk.login_service.auth

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
        @Column(nullable = false, length = 100) var email: String,
        @Column(nullable = false, length = 64) var selector: String, // public part
        @Column(nullable = false, length = 128) var verifierHash: String, // hash of secret
        @Column(nullable = false) var expiresAt: Instant,
        @Column(nullable = false) var consumed: Boolean = false,
        @Column(nullable = false) var createdAt: Instant = Instant.now()
)
