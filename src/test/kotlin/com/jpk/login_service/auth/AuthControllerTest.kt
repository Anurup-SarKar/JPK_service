package com.jpk.login_service.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.jpk.login_service.common.GlobalExceptionHandler
import com.jpk.login_service.common.JwtUtils
import com.jpk.login_service.common.Messages
import com.jpk.login_service.fixtures.TestFixtures
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var passwordResetService: PasswordResetService

    @MockBean
    private lateinit var jwtUtils: JwtUtils

    private val validSha256Hash = "a".repeat(64) // Valid 64-char SHA-256 hash

    @Test
    fun `login should return OTP response for valid credentials`() {
        val loginRequest = LoginRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash
        )
        val otpResponse = OtpResponse(otp = "123456", expiresInSeconds = 300)

        whenever(authService.loginAndGenerateOtp(any())).thenReturn(otpResponse)

        mockMvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.OTP_SENT))
            .andExpect(jsonPath("$.data.otp").value("123456"))
            .andExpect(jsonPath("$.data.expiresInSeconds").value(300))

        verify(authService).loginAndGenerateOtp(any())
    }

    @Test
    fun `login should return error for invalid credentials`() {
        val loginRequest = LoginRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash
        )

        whenever(authService.loginAndGenerateOtp(any()))
            .thenThrow(IllegalArgumentException(Messages.INVALID_CREDENTIALS))

        mockMvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value(Messages.INVALID_CREDENTIALS))
    }

    @Test
    @WithMockUser
    fun `validateOtp should return user session with JWT token`() {
        val otpValidateRequest = OtpValidateRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash,
            otp = "123456"
        )
        
        val user = TestFixtures.createUser(
            username = "testuser",
            email = "test@example.com",
            isAdmin = false,
            isActive = true
        )
        
        val userDataResponse = UserDataResponse(
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

        val jwtToken = "mock.jwt.token"

        whenever(authService.validateOtp(any())).thenReturn(userDataResponse)
        whenever(jwtUtils.generateToken(any(), any())).thenReturn(jwtToken)

        mockMvc.perform(
            post("/api/auth/validate-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpValidateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.OTP_VALIDATED))
            .andExpect(jsonPath("$.data.token").value(jwtToken))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.user.username").value("testuser"))
            .andExpect(jsonPath("$.data.user.email").value("test@example.com"))

        verify(authService).validateOtp(any())
        verify(jwtUtils).generateToken(eq("testuser"), any())
    }

    @Test
    fun `validateOtp should return error for invalid OTP`() {
        val validSha256Hash = "a".repeat(64)
        val otpValidateRequest = OtpValidateRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash,
            otp = "654321" // Valid format but wrong OTP
        )

        whenever(authService.validateOtp(any()))
            .thenThrow(IllegalArgumentException(Messages.OTP_INVALID))

        mockMvc.perform(
            post("/api/auth/validate-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpValidateRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value(Messages.OTP_INVALID))
    }

    @Test
    fun `validateOtp should return error for expired OTP`() {
        val otpValidateRequest = OtpValidateRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash,
            otp = "123456"
        )

        whenever(authService.validateOtp(any()))
            .thenThrow(IllegalArgumentException(Messages.OTP_EXPIRED))

        mockMvc.perform(
            post("/api/auth/validate-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpValidateRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value(Messages.OTP_EXPIRED))
    }

    @Test
    fun `resendOtp should return new OTP response`() {
        val loginRequest = LoginRequest(
            email = "test@example.com",
            passwordHash = validSha256Hash
        )
        val otpResponse = OtpResponse(otp = "654321", expiresInSeconds = 300)

        whenever(authService.resendOtp(any())).thenReturn(otpResponse)

        mockMvc.perform(
            post("/api/auth/resend-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.OTP_RESENT))
            .andExpect(jsonPath("$.data.otp").value("654321"))
            .andExpect(jsonPath("$.data.expiresInSeconds").value(300))

        verify(authService).resendOtp(any())
    }

    @Test
    fun `requestPasswordReset should return success response`() {
        val request = PasswordResetInitiateRequest(email = "test@example.com")
        val response = PasswordResetInitiateResponse(
            message = Messages.PASSWORD_RESET_REQUESTED,
            resetToken = "selector.verifier",
            expiresInSeconds = 900
        )

        whenever(passwordResetService.requestReset(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/auth/password/reset/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.PASSWORD_RESET_REQUESTED))
            .andExpect(jsonPath("$.data.resetToken").exists())

        verify(passwordResetService).requestReset("test@example.com")
    }

    @Test
    fun `performPasswordReset should reset password successfully`() {
        val request = PasswordResetPerformRequest(
            resetToken = "selector.verifier",
            email = "test@example.com",
            newPasswordHash = validSha256Hash
        )

        doNothing().whenever(passwordResetService).performReset(any())

        mockMvc.perform(
            post("/api/auth/password/reset/perform")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.PASSWORD_RESET_SUCCESS))

        verify(passwordResetService).performReset(any())
    }

    @Test
    fun `performPasswordReset should return error for invalid token`() {
        val request = PasswordResetPerformRequest(
            resetToken = "invalid.token",
            email = "test@example.com",
            newPasswordHash = validSha256Hash
        )

        whenever(passwordResetService.performReset(any()))
            .thenThrow(IllegalArgumentException("Invalid reset token"))

        mockMvc.perform(
            post("/api/auth/password/reset/perform")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `validateOtp should generate token with ADMIN role for admin user`() {
        val otpValidateRequest = OtpValidateRequest(
            email = "admin@example.com",
            passwordHash = validSha256Hash,
            otp = "123456"
        )
        
        val adminUser = TestFixtures.createUser(
            username = "adminuser",
            email = "admin@example.com",
            isAdmin = true,
            isActive = true
        )
        
        val userDataResponse = UserDataResponse(
            id = adminUser.id,
            username = adminUser.username,
            email = adminUser.email,
            mobile = adminUser.mobile,
            fullName = adminUser.fullName,
            cctvLink = adminUser.cctvLink,
            isCctvVisible = adminUser.isCctvVisible,
            isCctvStorageVisible = adminUser.isCctvStorageVisible,
            isAdmin = adminUser.isAdmin,
            isActive = adminUser.isActive
        )

        val jwtToken = "admin.jwt.token"

        whenever(authService.validateOtp(any())).thenReturn(userDataResponse)
        whenever(jwtUtils.generateToken(any(), any())).thenReturn(jwtToken)

        mockMvc.perform(
            post("/api/auth/validate-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpValidateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.user.isAdmin").value(true))

        verify(jwtUtils).generateToken(eq("adminuser"), argThat { claims ->
            claims["roles"] == "ADMIN"
        })
    }

    @Test
    @WithMockUser
    fun `validateOtp should generate token with INACTIVE role for inactive user`() {
        val otpValidateRequest = OtpValidateRequest(
            email = "inactive@example.com",
            passwordHash = validSha256Hash,
            otp = "123456"
        )
        
        val inactiveUser = TestFixtures.createUser(
            username = "inactiveuser",
            email = "inactive@example.com",
            isAdmin = false,
            isActive = false
        )
        
        val userDataResponse = UserDataResponse(
            id = inactiveUser.id,
            username = inactiveUser.username,
            email = inactiveUser.email,
            mobile = inactiveUser.mobile,
            fullName = inactiveUser.fullName,
            cctvLink = inactiveUser.cctvLink,
            isCctvVisible = inactiveUser.isCctvVisible,
            isCctvStorageVisible = inactiveUser.isCctvStorageVisible,
            isAdmin = inactiveUser.isAdmin,
            isActive = inactiveUser.isActive
        )

        val jwtToken = "inactive.jwt.token"

        whenever(authService.validateOtp(any())).thenReturn(userDataResponse)
        whenever(jwtUtils.generateToken(any(), any())).thenReturn(jwtToken)

        mockMvc.perform(
            post("/api/auth/validate-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otpValidateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.user.isActive").value(false))

        verify(jwtUtils).generateToken(eq("inactiveuser"), argThat { claims ->
            claims["roles"] == "INACTIVE"
        })
    }
}
