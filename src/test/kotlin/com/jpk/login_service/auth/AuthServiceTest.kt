package com.jpk.login_service.auth

import com.jpk.login_service.common.Messages
import com.jpk.login_service.common.PasswordUtils
import com.jpk.login_service.fixtures.TestFixtures
import com.jpk.login_service.user.UserRepository
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.mail.javamail.JavaMailSender
import java.time.Instant
import java.util.*

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var otpRepository: OtpRepository
    private lateinit var mailSender: JavaMailSender
    private lateinit var authService: AuthService

    private val fromAddress = "test@example.com"
    private val fromName = "Test Service"

    @BeforeEach
    fun setup() {
        userRepository = mock()
        otpRepository = mock()
        mailSender = mock()
        
        // Mock MimeMessage creation - return new instance for each call
        whenever(mailSender.createMimeMessage()).thenAnswer {
            MimeMessage(Session.getDefaultInstance(Properties()))
        }
        
        // Mock otpRepository.save to return the argument passed
        whenever(otpRepository.save(any<OtpEntry>())).thenAnswer { invocation ->
            invocation.getArgument<OtpEntry>(0)
        }
        
        authService = AuthService(
            userRepository,
            otpRepository,
            mailSender,
            fromAddress,
            fromName
        )
    }

    @Test
    fun `loginAndGenerateOtp should generate OTP for valid credentials`() {
        val email = "test@example.com"
        val rawPassword = "testPassword123"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(
            email = email,
            passwordHash = hashedPassword
        )
        
        val loginRequest = LoginRequest(email = email, passwordHash = sha256Hash)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        val response = authService.loginAndGenerateOtp(loginRequest)

        assertNotNull(response)
        assertNotNull(response.otp)
        assertEquals(6, response.otp.length)
        assertTrue(response.otp.all { it.isDigit() })
        assertEquals(300L, response.expiresInSeconds)
        
        verify(userRepository).findByEmail(email)
        verify(otpRepository).save(any<OtpEntry>())
        verify(mailSender).send(any<MimeMessage>())
    }

    @Test
    fun `loginAndGenerateOtp should throw exception for non-existent user`() {
        val email = "nonexistent@example.com"
        val loginRequest = LoginRequest(email = email, passwordHash = "anyPassword")

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            authService.loginAndGenerateOtp(loginRequest)
        }

        assertEquals(Messages.INVALID_CREDENTIALS, exception.message)
        verify(otpRepository, never()).save(any())
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `loginAndGenerateOtp should throw exception for incorrect password`() {
        val email = "test@example.com"
        val correctPassword = "correctPassword"
        val wrongPassword = "wrongPassword"
        
        val correctSha256 = PasswordUtils.generateSha256Hash(correctPassword)
        val wrongSha256 = PasswordUtils.generateSha256Hash(wrongPassword)
        val correctHashed = PasswordUtils.hashPassword(correctSha256)
        
        val user = TestFixtures.createUser(
            email = email,
            passwordHash = correctHashed
        )
        
        val loginRequest = LoginRequest(email = email, passwordHash = wrongSha256)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        val exception = assertThrows<IllegalArgumentException> {
            authService.loginAndGenerateOtp(loginRequest)
        }

        assertEquals(Messages.INVALID_CREDENTIALS, exception.message)
        verify(otpRepository, never()).save(any())
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `validateOtp should return user data for valid OTP`() {
        val email = "test@example.com"
        val rawPassword = "testPassword123"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        val otp = "123456"
        
        val user = TestFixtures.createUser(
            username = "testuser",
            email = email,
            passwordHash = hashedPassword
        )
        
        val otpEntry = TestFixtures.createOtpEntry(
            username = user.username,
            otp = otp,
            expiresAt = Instant.now().plusSeconds(300),
            consumed = false
        )
        
        val validateRequest = OtpValidateRequest(
            email = email,
            passwordHash = sha256Hash,
            otp = otp
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username))
            .thenReturn(otpEntry)

        val response = authService.validateOtp(validateRequest)

        assertNotNull(response)
        assertEquals(user.username, response.username)
        assertEquals(user.email, response.email)
        assertEquals(user.isAdmin, response.isAdmin)
        assertTrue(otpEntry.consumed)
        
        verify(otpRepository).save(otpEntry)
    }

    @Test
    fun `validateOtp should throw exception for expired OTP`() {
        val email = "test@example.com"
        val rawPassword = "password"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        val otp = "123456"
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val expiredOtpEntry = TestFixtures.createOtpEntry(
            username = user.username,
            otp = otp,
            expiresAt = Instant.now().minusSeconds(60),
            consumed = false
        )
        
        val validateRequest = OtpValidateRequest(
            email = email,
            passwordHash = sha256Hash,
            otp = otp
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username))
            .thenReturn(expiredOtpEntry)

        val exception = assertThrows<IllegalArgumentException> {
            authService.validateOtp(validateRequest)
        }

        assertEquals(Messages.OTP_EXPIRED, exception.message)
        assertFalse(expiredOtpEntry.consumed)
    }

    @Test
    fun `validateOtp should throw exception for incorrect OTP`() {
        val email = "test@example.com"
        val rawPassword = "password"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val otpEntry = TestFixtures.createOtpEntry(
            username = user.username,
            otp = "123456",
            expiresAt = Instant.now().plusSeconds(300),
            consumed = false
        )
        
        val validateRequest = OtpValidateRequest(
            email = email,
            passwordHash = sha256Hash,
            otp = "654321" // Wrong OTP
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username))
            .thenReturn(otpEntry)

        val exception = assertThrows<IllegalArgumentException> {
            authService.validateOtp(validateRequest)
        }

        assertEquals(Messages.OTP_INVALID, exception.message)
        assertFalse(otpEntry.consumed)
    }

    @Test
    fun `validateOtp should throw exception when no OTP found`() {
        val email = "test@example.com"
        val rawPassword = "password"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val validateRequest = OtpValidateRequest(
            email = email,
            passwordHash = sha256Hash,
            otp = "123456"
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username))
            .thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            authService.validateOtp(validateRequest)
        }

        assertEquals(Messages.OTP_NOT_FOUND, exception.message)
    }

    @Test
    fun `resendOtp should generate new OTP and send email`() {
        val email = "test@example.com"
        val rawPassword = "testPassword123"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val loginRequest = LoginRequest(email = email, passwordHash = sha256Hash)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        val response = authService.resendOtp(loginRequest)

        assertNotNull(response)
        assertNotNull(response.otp)
        assertEquals(6, response.otp.length)
        assertEquals(300L, response.expiresInSeconds)
        
        verify(otpRepository).save(any())
        verify(mailSender).send(any<MimeMessage>())
    }

    @Test
    fun `resendOtp should throw exception for invalid credentials`() {
        val email = "test@example.com"
        val correctPassword = "correctPassword"
        val wrongPassword = "wrongPassword"
        
        val correctSha256 = PasswordUtils.generateSha256Hash(correctPassword)
        val wrongSha256 = PasswordUtils.generateSha256Hash(wrongPassword)
        val correctHashed = PasswordUtils.hashPassword(correctSha256)
        
        val user = TestFixtures.createUser(email = email, passwordHash = correctHashed)
        val loginRequest = LoginRequest(email = email, passwordHash = wrongSha256)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        val exception = assertThrows<IllegalArgumentException> {
            authService.resendOtp(loginRequest)
        }

        assertEquals(Messages.INVALID_CREDENTIALS, exception.message)
        verify(otpRepository, never()).save(any())
        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `loginAndGenerateOtp should handle mail send failure gracefully`() {
        val email = "test@example.com"
        val rawPassword = "testPassword123"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val loginRequest = LoginRequest(email = email, passwordHash = sha256Hash)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(mailSender.send(any<MimeMessage>())).thenThrow(RuntimeException("Mail server down"))

        // Should not throw exception - mail failure is logged but not blocking
        val response = authService.loginAndGenerateOtp(loginRequest)

        assertNotNull(response)
        assertNotNull(response.otp)
        verify(otpRepository).save(any())
    }

    @Test
    fun `OTP should be 6 digits`() {
        val email = "test@example.com"
        val rawPassword = "password"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        
        val user = TestFixtures.createUser(email = email, passwordHash = hashedPassword)
        val loginRequest = LoginRequest(email = email, passwordHash = sha256Hash)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        val response = authService.loginAndGenerateOtp(loginRequest)

        assertEquals(6, response.otp.length)
        assertTrue(response.otp.toInt() in 100000..999999)
    }

    @Test
    fun `validateOtp should return correct user data structure`() {
        val email = "test@example.com"
        val rawPassword = "password"
        val sha256Hash = PasswordUtils.generateSha256Hash(rawPassword)
        val hashedPassword = PasswordUtils.hashPassword(sha256Hash)
        val otp = "123456"
        
        val user = TestFixtures.createUser(
            id = 1L,
            username = "testuser",
            email = email,
            mobile = "+919999999999",
            fullName = "Test User",
            passwordHash = hashedPassword,
            cctvLink = "http://cctv.example.com",
            isCctvVisible = true,
            isCctvStorageVisible = true,
            isAdmin = true,
            isActive = true
        )
        
        val otpEntry = TestFixtures.createOtpEntry(
            username = user.username,
            otp = otp,
            expiresAt = Instant.now().plusSeconds(300)
        )
        
        val validateRequest = OtpValidateRequest(email, sha256Hash, otp)

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(otpRepository.findTopByUsernameAndConsumedIsFalseOrderByCreatedAtDesc(user.username))
            .thenReturn(otpEntry)

        val response = authService.validateOtp(validateRequest)

        assertEquals(1L, response.id)
        assertEquals("testuser", response.username)
        assertEquals(email, response.email)
        assertEquals("+919999999999", response.mobile)
        assertEquals("Test User", response.fullName)
        assertEquals("http://cctv.example.com", response.cctvLink)
        assertTrue(response.isCctvVisible)
        assertTrue(response.isCctvStorageVisible)
        assertTrue(response.isAdmin)
        assertTrue(response.isActive)
    }
}
