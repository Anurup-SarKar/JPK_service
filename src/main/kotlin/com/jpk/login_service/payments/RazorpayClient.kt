package com.jpk.login_service.payments

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class RazorpayClient(
    @Value("\${razorpay.api.base-url}") private val baseUrl: String,
    @Value("\${razorpay.api.key-id}") private val keyId: String,
    @Value("\${razorpay.api.key-secret}") private val keySecret: String
) {
    private val webClient: WebClient by lazy {
        val creds = Base64.getEncoder().encodeToString("$keyId:$keySecret".toByteArray(StandardCharsets.UTF_8))
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $creds")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
            .build()
    }

    fun getPayments(from: Long?, to: Long?, count: Int?, skip: Int?): RazorpayPaymentsResponse {
        val params = mutableListOf<String>()
        if (from != null) params += "from=$from"
        if (to != null) params += "to=$to"
        if (count != null) params += "count=$count"
        if (skip != null) params += "skip=$skip"
        val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val uri = "/payments$query"

        return webClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus({ s -> s.is4xxClientError || s.is5xxServerError }) { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    RuntimeException("Razorpay error ${'$'}{resp.statusCode().value()}: $body")
                }
            }
            .bodyToMono(RazorpayPaymentsResponse::class.java)
            .block()!!
    }
}
