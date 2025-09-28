package com.jpk.login_service.common

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Instant
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtils(
        @Value("\${security.jwt.secret:dev-secret-change}") private val secret: String,
        @Value("\${security.jwt.issuer:login-service}") private val issuer: String,
        @Value("\${security.jwt.ttl-seconds:3600}") private val ttlSeconds: Long
) {
    private val algorithm: Algorithm by lazy { Algorithm.HMAC256(secret) }

    fun generateToken(subject: String, claims: Map<String, String> = emptyMap()): String {
        val now = Instant.now()
        val builder =
                JWT.create()
                        .withIssuer(issuer)
                        .withSubject(subject)
                        .withIssuedAt(Date.from(now))
                        .withExpiresAt(Date.from(now.plusSeconds(ttlSeconds)))
        claims.forEach { (k, v) -> builder.withClaim(k, v) }
        return builder.sign(algorithm)
    }

    fun verify(token: String): DecodedJWT =
            JWT.require(algorithm).withIssuer(issuer).build().verify(token)

    fun isExpired(token: String): Boolean =
            try {
                val jwt = verify(token)
                jwt.expiresAt?.toInstant()?.isBefore(Instant.now()) ?: true
            } catch (ex: Exception) {
                true
            }
}
