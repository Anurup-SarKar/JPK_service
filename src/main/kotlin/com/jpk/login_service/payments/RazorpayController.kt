package com.jpk.login_service.payments

import com.jpk.login_service.common.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/razorpay")
class RazorpayController(private val razorpayService: RazorpayService) {

    @GetMapping("/payments")
    fun listPayments(
        @RequestParam(required = false) from: Long?,
        @RequestParam(required = false) to: Long?,
        @RequestParam(required = false) count: Int?,
        @RequestParam(required = false) skip: Int?
    ): ResponseEntity<ApiResponse<RazorpayPaymentsResponse>> {
        val data = razorpayService.listPayments(from, to, count, skip)
        return ResponseEntity.ok(ApiResponse(statusCode = 200, statusMessage = "OK", data = data))
    }
}
