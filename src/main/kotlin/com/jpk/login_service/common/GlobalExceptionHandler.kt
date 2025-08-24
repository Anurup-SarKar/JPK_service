package com.jpk.login_service.common

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
            ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, String?>>> {
        val errors =
                ex.bindingResult.allErrors.associate { err ->
                    val field = (err as? FieldError)?.field ?: err.objectName
                    field to err.defaultMessage
                }
        val body =
                ApiResponse(
                        statusCode = 400,
                        statusMessage = Messages.VALIDATION_FAILED,
                        data = errors
                )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        val body =
                ApiResponse<Nothing>(
                        statusCode = 400,
                        statusMessage = ex.message ?: Messages.BAD_REQUEST
                )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(
            ex: DataIntegrityViolationException
    ): ResponseEntity<ApiResponse<Nothing>> {
        val msg =
                if ((ex.rootCause?.message ?: ex.message ?: "").contains(
                                "Duplicate",
                                ignoreCase = true
                        )
                ) {
                    Messages.USER_ALREADY_EXISTS
                } else Messages.BAD_REQUEST
        val body = ApiResponse<Nothing>(statusCode = 400, statusMessage = msg)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        val body = ApiResponse<Nothing>(statusCode = 500, statusMessage = Messages.INTERNAL_ERROR)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}
