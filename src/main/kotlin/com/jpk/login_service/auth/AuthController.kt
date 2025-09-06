package com.jpk.login_service.auth

import com.jpk.login_service.common.ApiResponse
import com.jpk.login_service.common.Messages
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/auth")
class AuthController(
        private val authService: AuthService,
        private val passwordResetService: PasswordResetService
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
        ): ResponseEntity<ApiResponse<UserDataResponse>> =
                ResponseEntity.ok(
                        ApiResponse(
                                statusCode = 200,
                                statusMessage = Messages.OTP_VALIDATED,
                                data = authService.validateOtp(req)
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
