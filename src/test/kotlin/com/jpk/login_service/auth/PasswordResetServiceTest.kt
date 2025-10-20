package com.jpk.login_service.auth

import com.jpk.login_service.common.Messages
import com.jpk.login_service.fixtures.TestFixtures
import com.jpk.login_service.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.mail.javamail.JavaMailSender
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import java.time.Duration
import java.time.Instant
import java.util.*

class PasswordResetServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var mailSender: JavaMailSender
    private lateinit var passwordResetService: PasswordResetService

    private val fromAddress = "test@example.com"
    private val fromName = "Test Service"
    private val frontendBaseUrl = "https://test.example.com"
    private val resetPath = "/reset"

    @BeforeEach
    fun setup() {
        userRepository = mock()
        passwordResetTokenRepository = mock()
        mailSender = mock()
        
        // Mock MimeMessage creation - return new instance for each call
        whenever(mailSender.createMimeMessage()).thenAnswer {
            MimeMessage(Session.getDefaultInstance(Properties()))
        }
        
        // Mock passwordResetTokenRepository.save to return the argument
        whenever(passwordResetTokenRepository.save(any<PasswordResetToken>())).thenAnswer { invocation ->
            invocation.getArgument<PasswordResetToken>(0)
        }
        
        passwordResetService = PasswordResetService(
            userRepository,
            passwordResetTokenRepository,
            mailSender,
            fromAddress,
            fromName,
            frontendBaseUrl,
            resetPath
        )
    }

    @Test
    fun `requestReset should create token and send email for valid user`() {
        val email = "user@example.com"
        val user = TestFixtures.createUser(email = email)
        
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email))
            .thenReturn(null)

        val response = passwordResetService.requestReset(email)

        assertNotNull(response)
        assertEquals(Messages.PASSWORD_RESET_REQUESTED, response.message)
        assertNotNull(response.resetToken)
        assertTrue(response.resetToken.contains("."))
        assertEquals(Duration.ofMinutes(15).seconds, response.expiresInSeconds)
        
        verify(passwordResetTokenRepository).save(any())
        verify(mailSender).send(any<MimeMessage>())
    }

    @Test
    fun `requestReset should invalidate old token before creating new one`() {
        val email = "user@example.com"
        val user = TestFixtures.createUser(email = email)
        
        val oldToken = TestFixtures.createPasswordResetToken(
            email = email,
            selector = "oldSelector",
            verifierHash = "oldHash"
        )
        
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email))
            .thenReturn(oldToken)

        passwordResetService.requestReset(email)

        assertTrue(oldToken.consumed)
        verify(passwordResetTokenRepository, times(2)).save(any())
    }

    @Test
    fun `requestReset should throw exception for non-existent user`() {
        val email = "nonexistent@example.com"
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            passwordResetService.requestReset(email)
        }
        
        assertEquals(Messages.USER_NOT_FOUND, exception.message)
        verify(passwordResetTokenRepository, never()).save(any())
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `performReset should update password for valid token`() {
        val email = "user@example.com"
        val selector = "testSelector"
        val secret = "testSecret123456789012345678901234567890123456789012345678901234"
        val resetToken = "$selector.$secret"
        
        val user = TestFixtures.createUser(
            email = email,
            passwordHash = "oldHashedPassword"
        )
        
        val token = TestFixtures.createPasswordResetToken(
            email = email,
            selector = selector,
            verifierHash = com.jpk.login_service.common.PasswordUtils.sha256Hex(secret),
            expiresAt = Instant.now().plusSeconds(300)
        )
        
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email))
            .thenReturn(token)
        whenever(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

        val request = PasswordResetPerformRequest(
            email = email,
            resetToken = resetToken,
            newPasswordHash = "newPasswordSha256Hash"
        )

        passwordResetService.performReset(request)

        assertTrue(token.consumed)
        assertNotEquals("oldHashedPassword", user.passwordHash)
        verify(passwordResetTokenRepository).save(token)
        verify(userRepository).save(user)
    }

    @Test
    fun `performReset should throw exception for invalid token format`() {
        val request = PasswordResetPerformRequest(
            email = "user@example.com",
            resetToken = "invalidToken",
            newPasswordHash = "newHash"
        )

        val exception = assertThrows<IllegalArgumentException> {
            passwordResetService.performReset(request)
        }
        
        assertEquals(Messages.PASSWORD_RESET_TOKEN_INVALID, exception.message)
    }

    @Test
    fun `performReset should throw exception for expired token`() {
        val email = "user@example.com"
        val selector = "testSelector"
        val secret = "testSecret123456789012345678901234567890123456789012345678901234"
        val resetToken = "$selector.$secret"
        
        val token = TestFixtures.createPasswordResetToken(
            email = email,
            selector = selector,
            verifierHash = com.jpk.login_service.common.PasswordUtils.sha256Hex(secret),
            expiresAt = Instant.now().minusSeconds(100)
        )
        
        whenever(passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email))
            .thenReturn(token)

        val request = PasswordResetPerformRequest(
            email = email,
            resetToken = resetToken,
            newPasswordHash = "newHash"
        )

        val exception = assertThrows<IllegalArgumentException> {
            passwordResetService.performReset(request)
        }
        
        assertEquals(Messages.PASSWORD_RESET_TOKEN_INVALID, exception.message)
    }

    @Test
    fun `performReset should throw exception for mismatched selector`() {
        val email = "user@example.com"
        val selector = "testSelector"
        val secret = "testSecret123456789012345678901234567890123456789012345678901234"
        val resetToken = "wrongSelector.$secret"
        
        val token = TestFixtures.createPasswordResetToken(
            email = email,
            selector = selector,
            verifierHash = com.jpk.login_service.common.PasswordUtils.sha256Hex(secret),
            expiresAt = Instant.now().plusSeconds(300)
        )
        
        whenever(passwordResetTokenRepository.findTopByEmailAndConsumedIsFalseOrderByCreatedAtDesc(email))
            .thenReturn(token)

        val request = PasswordResetPerformRequest(
            email = email,
            resetToken = resetToken,
            newPasswordHash = "newHash"
        )

        val exception = assertThrows<IllegalArgumentException> {
            passwordResetService.performReset(request)
        }
        
        assertEquals(Messages.PASSWORD_RESET_TOKEN_INVALID, exception.message)
    }
}
