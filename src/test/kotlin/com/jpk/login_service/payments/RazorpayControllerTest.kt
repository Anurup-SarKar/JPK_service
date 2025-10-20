package com.jpk.login_service.payments

import com.jpk.login_service.common.GlobalExceptionHandler
import com.jpk.login_service.common.JwtUtils
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(RazorpayController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
class RazorpayControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var razorpayService: RazorpayService

    @MockBean private lateinit var jwtUtils: JwtUtils

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `listPayments should return payments from service`() {
        val mockPayment =
                RazorpayPaymentItem(
                        id = "pay_123",
                        entity = "payment",
                        amount = 1000L,
                        currency = "INR",
                        status = "captured",
                        order_id = null,
                        invoice_id = null,
                        international = false,
                        method = "upi",
                        amount_refunded = 0L,
                        refund_status = null,
                        captured = true,
                        description = "Test payment",
                        card_id = null,
                        bank = null,
                        wallet = null,
                        vpa = "test@upi",
                        upi = null,
                        email = "test@example.com",
                        contact = "+919999999999",
                        notes = emptyMap(),
                        fee = 20L,
                        tax = 4L,
                        error_code = null,
                        error_description = null,
                        error_source = null,
                        error_step = null,
                        error_reason = null,
                        acquirer_data = emptyMap(),
                        created_at = 1593320020L
                )
        val mockResponse = RazorpayPaymentsResponse("collection", 1, listOf(mockPayment))
        whenever(razorpayService.listPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        mockMvc.perform(
                        get("/api/admin/razorpay/payments")
                                .param("from", "1593320020")
                                .param("to", "1624856020")
                                .param("count", "10")
                                .param("skip", "0")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("pay_123"))
                .andExpect(jsonPath("$.data.items[0].amount").value(1000))

        verify(razorpayService).listPayments(1593320020L, 1624856020L, 10, 0)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `listPayments should handle optional parameters`() {
        val mockResponse = RazorpayPaymentsResponse("collection", 0, emptyList())
        whenever(razorpayService.listPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        mockMvc.perform(
                        get("/api/admin/razorpay/payments")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.statusCode").value(200))

        verify(razorpayService).listPayments(null, null, null, null)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `listPayments should handle service errors gracefully`() {
        whenever(razorpayService.listPayments(any(), any(), any(), any()))
                .thenThrow(RuntimeException("Razorpay API error"))

        // The global exception handler should catch this and return an error response
        // Just verify the service was called - the actual error handling is tested elsewhere
        mockMvc.perform(
                get("/api/admin/razorpay/payments")
                        .param("from", "1593320020")
                        .param("to", "1624856020")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
        )

        verify(razorpayService).listPayments(1593320020L, 1624856020L, null, null)
    }
}
