package com.jpk.login_service.payments

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class RazorpayServiceTest {

    private val razorpayClient = mock<RazorpayClient>()
    private val razorpayService = RazorpayService(razorpayClient)

    @Test
    fun `listPayments should call client with provided parameters`() {
        val mockResponse = RazorpayPaymentsResponse("collection", 1, emptyList())
        whenever(razorpayClient.getPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        val result = razorpayService.listPayments(1593320020, 1624856020, 10, 0)

        assertNotNull(result)
        assertEquals("collection", result.entity)
        verify(razorpayClient).getPayments(1593320020, 1624856020, 10, 0)
    }

    @Test
    fun `listPayments should apply default count when null`() {
        val mockResponse = RazorpayPaymentsResponse("collection", 0, emptyList())
        whenever(razorpayClient.getPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        razorpayService.listPayments(null, null, null, null)

        verify(razorpayClient).getPayments(null, null, 10, 0)
    }

    @Test
    fun `listPayments should coerce count within valid range`() {
        val mockResponse = RazorpayPaymentsResponse("collection", 0, emptyList())
        whenever(razorpayClient.getPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        razorpayService.listPayments(null, null, 200, null)

        verify(razorpayClient).getPayments(null, null, 100, 0)
    }

    @Test
    fun `listPayments should throw when from is out of range`() {
        val exception = assertThrows<IllegalArgumentException> {
            razorpayService.listPayments(100, 1624856020, 10, 0)
        }
        assertTrue(exception.message!!.contains("from must be between"))
    }

    @Test
    fun `listPayments should throw when to is out of range`() {
        val exception = assertThrows<IllegalArgumentException> {
            razorpayService.listPayments(1593320020, 5000000000, 10, 0)
        }
        assertTrue(exception.message!!.contains("to must be between"))
    }

    @Test
    fun `listPayments should throw when from is greater than to`() {
        val exception = assertThrows<IllegalArgumentException> {
            razorpayService.listPayments(1624856020, 1593320020, 10, 0)
        }
        assertTrue(exception.message!!.contains("from must be <= to"))
    }

    @Test
    fun `listPayments should handle negative skip by coercing to zero`() {
        val mockResponse = RazorpayPaymentsResponse("collection", 0, emptyList())
        whenever(razorpayClient.getPayments(any(), any(), any(), any())).thenReturn(mockResponse)

        razorpayService.listPayments(null, null, null, -5)

        verify(razorpayClient).getPayments(null, null, 10, 0)
    }
}
