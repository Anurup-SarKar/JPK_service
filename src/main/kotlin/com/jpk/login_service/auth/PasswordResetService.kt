package com.jpk.login_service.auth

import com.jpk.login_service.common.Messages
import com.jpk.login_service.common.PasswordUtils
import com.jpk.login_service.user.UserRepository
import java.time.Duration
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import jakarta.mail.internet.InternetAddress
import java.nio.charset.StandardCharsets
import java.net.URLEncoder
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory

@Service
class PasswordResetService(
        private val userRepository: UserRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository,
        private val mailSender: JavaMailSender,
        @Value("\${app.mail.fromAddress:jpkadmin@jpkindia.org}") private val fromAddress: String,
        @Value("\${app.mail.fromName:JPK India}") private val fromName: String,
        @Value("\${app.frontend.base-url:https://jpkindia.org}") private val frontendBaseUrl: String,
        @Value("\${app.frontend.reset-path:/reset_password}") private val resetPath: String
) {
    private val log = LoggerFactory.getLogger(PasswordResetService::class.java)
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
        val combined = "$selector.$secret"

        // Send reset email with hyperlink and expiry details
        sendPasswordResetMail(user.email, combined, ttl)

        return PasswordResetInitiateResponse(
                message = Messages.PASSWORD_RESET_REQUESTED,
                resetToken = combined,
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

    private fun sendPasswordResetMail(email: String, resetToken: String, ttl: Duration) {
        try {
            val minutes = ttl.toMinutes()
            val encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8)
            val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8)
            // Deep link: /reset_password?token=<resetToken>&email=<email>
            val link = "$frontendBaseUrl$resetPath?token=$encodedToken&email=$encodedEmail"
            // HTML-safe href (escape '&' to '&amp;' for email clients)
            val htmlHref = link.replace("&", "&amp;")

            val subject = "Reset your JPK India password"
            val textBody = """
                Hello,

                We received a request to reset the password for your JPK India account.

                To reset your password, open the link below:
                $link

                This link expires in $minutes minutes.

                If you did not request a password reset, you can safely ignore this email.

                Do not reply to this email. This mailbox is not monitored.

                — JPK India
            """.trimIndent()

            val htmlBody = """
                <div style="font-family:Roboto,Helvetica,Arial,sans-serif;line-height:1.6;color:#111;">
                  <p>Hello,</p>
                  <p>We received a request to reset the password for your <strong>JPK India</strong> account.</p>
                  <p>To reset your password, click the link below:</p>
                  <p style="margin:16px 0;">
                    <a href="$htmlHref" style="color:#2563eb;text-decoration:underline;font-weight:600;" target="_blank" rel="noopener noreferrer">Password Reset link</a>
                  </p>
                  <p style="margin-top:12px;color:#374151;">If the link doesn't work, copy and paste this URL into your browser:</p>
                  <p style="word-break:break-all;background:#f3f4f6;border:1px solid #e5e7eb;border-radius:8px;padding:12px;">$htmlHref</p>
                  <p style="margin-top:12px;color:#374151;">This link expires in <strong>$minutes minutes</strong>.</p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:16px 0;" />
                  <p style="color:#6b7280;font-size:13px;"><strong>Do not reply</strong> to this email. This mailbox is not monitored.</p>
                  <p style="color:#6b7280;font-size:13px;">— JPK India</p>
                </div>
            """.trimIndent()

            val mime = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name())
            helper.setTo(email)
            helper.setFrom(InternetAddress(fromAddress, fromName))
            helper.setSubject(subject)
            helper.setText(textBody, htmlBody)

            mime.addHeader("Auto-Submitted", "auto-generated")
            mime.addHeader("Precedence", "bulk")
            mime.addHeader("X-Auto-Response-Suppress", "All")

            mailSender.send(mime)
            log.info("✓ Password reset email sent to {}", email)
        } catch (ex: Exception) {
            log.error(
                "✗ Failed to send password reset email to {}: {} - {}",
                email,
                ex.javaClass.simpleName,
                ex.message
            )
            log.debug("Full exception stack:", ex)
            // Do not fail the flow on email errors
        }
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
