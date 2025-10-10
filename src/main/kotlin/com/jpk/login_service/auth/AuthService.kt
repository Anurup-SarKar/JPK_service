package com.jpk.login_service.auth

import com.jpk.login_service.common.Messages
import com.jpk.login_service.common.PasswordUtils
import com.jpk.login_service.user.UserRepository
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import jakarta.mail.internet.InternetAddress
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val otpRepository: OtpRepository,
        private val mailSender: JavaMailSender,
        @Value("\${app.mail.fromAddress}") private val fromAddress: String,
        @Value("\${app.mail.fromName}") private val fromName: String
) {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
        private val otpTtl: Duration = Duration.ofMinutes(Messages.OTP_TTL_MINUTES)

        @Transactional
        fun loginAndGenerateOtp(req: LoginRequest): OtpResponse {
                val user =
                        userRepository.findByEmail(req.email).orElseThrow {
                                IllegalArgumentException(Messages.INVALID_CREDENTIALS)
                        }
                val supplied = req.passwordHash
                try {
                        if (!PasswordUtils.verifyPassword(supplied, user.passwordHash)) {
                                throw IllegalArgumentException(Messages.INVALID_CREDENTIALS)
                        }
                } catch (e: IllegalArgumentException) {
                        // rethrow (validation issue already meaningful)
                        throw e
                } catch (e: Exception) {
                        // Wrap unexpected errors so they don't bubble as 500
                        throw IllegalArgumentException(Messages.INVALID_CREDENTIALS)
                }

                val otp = Random.nextInt(100000, 1000000).toString()
                val entry =
                        OtpEntry(
                                username = user.username,
                                otp = otp,
                                expiresAt = Instant.now().plus(otpTtl)
                        )
                otpRepository.save(entry)
                sendOtpMail(user.email, otp)
                return OtpResponse(otp = otp, expiresInSeconds = otpTtl.seconds)
        }

        @Transactional
        fun validateOtp(req: OtpValidateRequest): UserDataResponse {
                val user =
                        userRepository.findByEmail(req.email).orElseThrow {
                                IllegalArgumentException(Messages.INVALID_CREDENTIALS)
                        }
                if (!PasswordUtils.verifyPassword(req.passwordHash, user.passwordHash))
                        throw IllegalArgumentException(Messages.INVALID_CREDENTIALS)

                val otpEntry =
                        otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(
                                user.username
                        )
                                ?: throw IllegalArgumentException(Messages.OTP_NOT_FOUND)
                if (otpEntry.expiresAt.isBefore(Instant.now()))
                        throw IllegalArgumentException(Messages.OTP_EXPIRED)
                if (otpEntry.otp != req.otp) throw IllegalArgumentException(Messages.OTP_INVALID)

                otpEntry.consumed = true
                otpRepository.save(otpEntry)

                return UserDataResponse(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        mobile = user.mobile,
                        fullName = user.fullName,
                        cctvLink = user.cctvLink,
                        isCctvVisible = user.isCctvVisible,
                        isCctvStorageVisible = user.isCctvStorageVisible,
                        isAdmin = user.isAdmin,
                        isActive = user.isActive
                )
        }

        @Transactional
        fun resendOtp(req: LoginRequest): OtpResponse {
                val user =
                        userRepository.findByEmail(req.email).orElseThrow {
                                IllegalArgumentException(Messages.INVALID_CREDENTIALS)
                        }
                if (!PasswordUtils.verifyPassword(req.passwordHash, user.passwordHash))
                        throw IllegalArgumentException(Messages.INVALID_CREDENTIALS)

                val otp = Random.nextInt(100000, 1000000).toString()
                val entry =
                        OtpEntry(
                                username = user.username,
                                otp = otp,
                                expiresAt = Instant.now().plus(otpTtl)
                        )
                otpRepository.save(entry)
                sendOtpMail(user.email, otp)
                return OtpResponse(otp = otp, expiresInSeconds = otpTtl.seconds)
        }

        private fun sendOtpMail(email: String, otp: String) {
                log.info("=== sendOtpMail called for email: {} with OTP: {}", email, otp)
                try {
                        val subject = "Your JPK India login OTP – Do Not Reply"
                        val minutes = otpTtl.toMinutes()
                        val textBody = """
                                Hello,

                                Use the One-Time Password (OTP) below to complete your login to JPK India:

                                OTP: $otp
                                Expires in: $minutes minutes

                                If you did not request this code, you can ignore this message.

                                Do not reply to this email. This mailbox is not monitored.

                                — JPK India
                        """.trimIndent()

                        val htmlBody = """
                                <div style=\"font-family:Roboto,Helvetica,Arial,sans-serif;line-height:1.6;color:#111;\">
                                  <p>Hello,</p>
                                  <p>Use the One-Time Password (OTP) below to complete your login to <strong>JPK India</strong>:</p>
                                  <p style=\"font-size:22px;font-weight:700;letter-spacing:2px;background:#f3f4f6;border:1px solid #e5e7eb;border-radius:8px;padding:12px 16px;display:inline-block;\">
                                    $otp
                                  </p>
                                  <p style=\"margin-top:12px;color:#374151;\">This code expires in <strong>$minutes minutes</strong>.</p>
                                  <p>If you did not request this code, you can safely ignore this email.</p>
                                  <hr style=\"border:none;border-top:1px solid #e5e7eb;margin:16px 0;\" />
                                  <p style=\"color:#6b7280;font-size:13px;\"><strong>Do not reply</strong> to this email. This mailbox is not monitored.</p>
                                  <p style=\"color:#6b7280;font-size:13px;\">— JPK India</p>
                                </div>
                        """.trimIndent()

                        log.info("Creating MIME message with fromAddress: {} and fromName: {}", fromAddress, fromName)
                        val mime = mailSender.createMimeMessage()
                        val helper = MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name()) // multipart=true for HTML+text
                        helper.setTo(email)
                        helper.setFrom(InternetAddress(fromAddress, fromName))
                        helper.setSubject(subject)
                        helper.setText(textBody, htmlBody)

                        // Helpful headers for auto-replies
                        mime.addHeader("Auto-Submitted", "auto-generated")
                        mime.addHeader("Precedence", "bulk")
                        mime.addHeader("X-Auto-Response-Suppress", "All")

                        log.info("About to send MIME message via mailSender...")
                        mailSender.send(mime)
                        log.info("✓ OTP email sent successfully to {}", email)
                } catch (ex: Exception) {
                        log.error("✗ Failed to send OTP email to {}: {} - {}", email, ex.javaClass.simpleName, ex.message)
                        log.debug("Full exception stack:", ex)
                        // Swallow for now to not block login flow; consider alerting in prod
                }
        }
}
