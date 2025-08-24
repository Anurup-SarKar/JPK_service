package com.jpk.login_service.auth

import com.jpk.login_service.common.ApiResponse
import com.jpk.login_service.common.Messages
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

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
    fun resendOtp(@Valid @RequestBody req: LoginRequest): ResponseEntity<ApiResponse<OtpResponse>> =
            ResponseEntity.ok(
                    ApiResponse(
                            statusCode = 200,
                            statusMessage = Messages.OTP_RESENT,
                            data = authService.resendOtp(req)
                    )
            )
}
