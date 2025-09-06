package com.jpk.login_service.auth

import com.jpk.login_service.common.Messages
import com.jpk.login_service.common.PasswordUtils
import com.jpk.login_service.user.UserRepository
import java.time.Duration
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PasswordResetService(
        private val userRepository: UserRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository
) {
    private val ttl: Duration = Duration.ofMinutes(15)

    @Transactional
    fun requestReset(email: String): PasswordResetInitiateResponse {
        val user =
                userRepository.findByEmail(email).orElseThrow {
                    IllegalArgumentException(Messages.USER_NOT_FOUND)
                }
        // Invalidate old tokens (optionally mark consumed)
        passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email)
                ?.let {
                    it.consumed = true
                    passwordResetTokenRepository.save(it)
                }
        val selector = PasswordUtils.randomHex(8) // 16 hex chars
        val secret = PasswordUtils.randomHex(32) // 64 hex chars
        val verifierHash = PasswordUtils.sha256Hex(secret)
        val token =
                PasswordResetToken(
                        email = user.email,
                        selector = selector,
                        verifierHash = verifierHash,
                        expiresAt = Instant.now().plus(ttl)
                )
        passwordResetTokenRepository.save(token)
        // Return combined token: selector.secret (dot separated)
        return PasswordResetInitiateResponse(
                message = Messages.PASSWORD_RESET_REQUESTED,
                resetToken = "$selector.$secret",
                expiresInSeconds = ttl.seconds
        )
    }

    @Transactional
    fun performReset(req: PasswordResetPerformRequest) {
        val parts = req.resetToken.split('.')
        if (parts.size != 2) throw IllegalArgumentException(Messages.PASSWORD_RESET_TOKEN_INVALID)
        val (selector, secret) = parts
        val token =
                passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(
                        req.email
                )
                        ?: throw IllegalArgumentException(Messages.PASSWORD_RESET_TOKEN_INVALID)
        if (token.selector != selector)
                throw IllegalArgumentException(Messages.PASSWORD_RESET_TOKEN_INVALID)
        if (token.expiresAt.isBefore(Instant.now()))
                throw IllegalArgumentException(Messages.PASSWORD_RESET_TOKEN_INVALID)
        val expectedHash = PasswordUtils.sha256Hex(secret)
        if (expectedHash != token.verifierHash)
                throw IllegalArgumentException(Messages.PASSWORD_RESET_TOKEN_INVALID)
        val user =
                userRepository.findByEmail(req.email).orElseThrow {
                    IllegalArgumentException(Messages.USER_NOT_FOUND)
                }
        // req.newPasswordHash is SHA-256 of raw password from FE
        val newBcrypt = PasswordUtils.hashPassword(req.newPasswordHash)
        user.passwordHash = newBcrypt
        token.consumed = true
        passwordResetTokenRepository.save(token)
        userRepository.save(user)
    }
}

data class PasswordResetInitiateRequest(val email: String)

data class PasswordResetInitiateResponse(
        val message: String,
        val resetToken: String,
        val expiresInSeconds: Long
)

data class PasswordResetPerformRequest(
        val email: String,
        val resetToken: String,
        val newPasswordHash: String // SHA-256 from FE
)
