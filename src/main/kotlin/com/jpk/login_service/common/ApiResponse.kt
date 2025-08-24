package com.jpk.login_service.common

// Generic API response wrapper
data class ApiResponse<T>(val statusCode: Int, val statusMessage: String, val data: T? = null)
