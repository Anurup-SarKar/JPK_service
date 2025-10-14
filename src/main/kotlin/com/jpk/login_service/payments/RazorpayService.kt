package com.jpk.login_service.payments

import org.springframework.stereotype.Service

@Service
class RazorpayService(private val razorpayClient: RazorpayClient) {
    fun listPayments(from: Long?, to: Long?, count: Int?, skip: Int?): RazorpayPaymentsResponse {
        if (from != null) require(from in 946_684_800..4_765_046_400) { "from must be between 946684800 and 4765046400" }
        if (to != null) require(to in 946_684_800..4_765_046_400) { "to must be between 946684800 and 4765046400" }
        if (from != null && to != null) require(from <= to) { "from must be <= to" }
        val safeCount = (count ?: 10).coerceIn(1, 100) // default 10
        val safeSkip = (skip ?: 0).coerceAtLeast(0)
        return razorpayClient.getPayments(from, to, safeCount, safeSkip)
    }
}
