package com.jpk.login_service.auth

import com.jpk.login_service.common.ApiResponse
import com.jpk.login_service.common.JwtUtils
import com.jpk.login_service.common.Messages
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/auth")
class AuthController(
        private val authService: AuthService,
        private val passwordResetService: PasswordResetService,
        private val jwtUtils: JwtUtils
) {

        @PostMapping("/login")
        fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<ApiResponse<OtpResponse>> =
                ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.OTP_SENT,
                                data = authService.loginAndGenerateOtp(req)
                        )
                )

        @PostMapping("/validate-otp")
        fun validateOtp(
                @Valid @RequestBody req: OtpValidateRequest
        ): ResponseEntity<ApiResponse<UserSessionResponse>> =
                ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.OTP_VALIDATED,
                                data =
                                        authService.validateOtp(req).let { user ->
                                                val token =
                                                        jwtUtils.generateToken(
                                                                subject = user.username,
                                                                claims =
                                                                        mapOf(
                                                                                "email" to
                                                                                        user.email,
                                                                                "isAdmin" to
                                                                                        user.isAdmin
                                                                                                .toString(),
                                                                                "roles" to
                                                                                        (if (!user.isActive
                                                                                        )
                                                                                                "INACTIVE"
                                                                                        else if (user.isAdmin
                                                                                        )
                                                                                                "ADMIN"
                                                                                        else "USER")
                                                                        )
                                                        )
                                                UserSessionResponse(
                                                        user = user,
                                                        token = token,
                                                        tokenType = "Bearer"
                                                )
                                        }
                        )
                )

        @PostMapping("/resend-otp")
        fun resendOtp(
                @Valid @RequestBody req: LoginRequest
        ): ResponseEntity<ApiResponse<OtpResponse>> =
                ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.OTP_RESENT,
                                data = authService.resendOtp(req)
                        )
                )

        @PostMapping("/password/reset/request")
        fun requestPasswordReset(
                @RequestBody req: PasswordResetInitiateRequest
        ): ResponseEntity<ApiResponse<PasswordResetInitiateResponse>> =
                ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.PASSWORD_RESET_REQUESTED,
                                data = passwordResetService.requestReset(req.email)
                        )
                )

        @PostMapping("/password/reset/perform")
        fun performPasswordReset(
                @RequestBody req: PasswordResetPerformRequest
        ): ResponseEntity<ApiResponse<Void>> {
                passwordResetService.performReset(req)
                return ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.PASSWORD_RESET_SUCCESS,
                                data = null
                        )
                )
        }
}

data class UserSessionResponse(
        val user: UserDataResponse,
        val token: String,
        val tokenType: String
)
