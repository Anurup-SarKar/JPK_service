package com.jpk.login_service.payments

// Minimal DTOs to map Razorpay payments list

data class RazorpayPaymentsResponse(
    val entity: String?,
    val count: Int?,
    val items: List<RazorpayPaymentItem>?
)

data class RazorpayPaymentItem(
    val id: String?,
    val entity: String?,
    val amount: Long?,
    val currency: String?,
    val status: String?,
    val order_id: String?,
    val invoice_id: String?,
    val international: Boolean?,
    val method: String?,
    val amount_refunded: Long?,
    val refund_status: String?,
    val captured: Boolean?,
    val description: String?,
    val card_id: String?,
    val bank: String?,
    val wallet: String?,
    val vpa: String?,
    val upi: Map<String, Any>?,
    val email: String?,
    val contact: String?,
    val notes: Map<String, Any>?,
    val fee: Long?,
    val tax: Long?,
    val error_code: String?,
    val error_description: String?,
    val error_source: String?,
    val error_step: String?,
    val error_reason: String?,
    val acquirer_data: Map<String, Any>?,
    val created_at: Long?
)
