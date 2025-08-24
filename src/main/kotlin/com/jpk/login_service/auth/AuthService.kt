package com.jpk.login_service.auth

import com.jpk.login_service.user.UserRepository
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val otpRepository: OtpRepository,
        private val mailSender: org.springframework.mail.javamail.JavaMailSender
) {
    private val otpTtl: Duration =
            Duration.ofMinutes(com.jpk.login_service.common.Messages.OTP_TTL_MINUTES)

    @Transactional
    fun loginAndGenerateOtp(req: LoginRequest): OtpResponse {
        val user =
                userRepository.findByEmail(req.email).orElseThrow {
                    IllegalArgumentException(
                            com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                    )
                }
        if (req.password != user.passwordHash)
                throw IllegalArgumentException(
                        com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                )

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
                    IllegalArgumentException(
                            com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                    )
                }
        if (req.password != user.passwordHash)
                throw IllegalArgumentException(
                        com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                )

        val otpEntry =
                otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username)
                        ?: throw IllegalArgumentException(
                                com.jpk.login_service.common.Messages.OTP_NOT_FOUND
                        )
        if (otpEntry.expiresAt.isBefore(Instant.now()))
                throw IllegalArgumentException(com.jpk.login_service.common.Messages.OTP_EXPIRED)
        if (otpEntry.otp != req.otp)
                throw IllegalArgumentException(com.jpk.login_service.common.Messages.OTP_INVALID)

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
                isActive = user.isActive
        )
    }

    @Transactional
    fun resendOtp(req: LoginRequest): OtpResponse {
        val user =
                userRepository.findByEmail(req.email).orElseThrow {
                    IllegalArgumentException(
                            com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                    )
                }
        if (req.password != user.passwordHash)
                throw IllegalArgumentException(
                        com.jpk.login_service.common.Messages.INVALID_CREDENTIALS
                )

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
        try {
            val message = org.springframework.mail.SimpleMailMessage()
            message.setTo(email)
            message.subject = com.jpk.login_service.common.Messages.MAIL_OTP_SUBJECT
            message.text =
                    com.jpk.login_service.common.Messages.MAIL_OTP_BODY_TEMPLATE.format(
                            otp,
                            otpTtl.toMinutes()
                    )
            mailSender.send(message)
        } catch (ex: Exception) {
            // log error in real scenario
        }
    }
}
