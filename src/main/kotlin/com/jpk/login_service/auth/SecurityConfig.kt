package com.jpk.login_service.auth

import com.jpk.login_service.common.JwtUtils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class SecurityConfig(private val jwtUtils: JwtUtils) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
                .csrf { it.disable() }
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
                .authorizeHttpRequests { auth ->
                    auth.requestMatchers(
                                    "/api/auth/login",
                                    "/api/auth/validate-otp",
                                    "/api/auth/resend-otp",
                                    "/api/auth/password/reset/**"
                            )
                            .permitAll()
                            .anyRequest()
                            .authenticated()
                }
                .addFilterBefore(
                        JwtAuthFilter(jwtUtils),
                        UsernamePasswordAuthenticationFilter::class.java
                )
        return http.build()
    }
}

class JwtAuthFilter(private val jwtUtils: JwtUtils) : OncePerRequestFilter() {
    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substringAfter("Bearer ")
            try {
                val decoded = jwtUtils.verify(token)
                if (!jwtUtils.isExpired(token)) {
                    val username = decoded.subject
                    val roles =
                            decoded.getClaim("roles")
                                    ?.asArray(String::class.java)
                                    ?.toList()
                                    .orEmpty()
                    val auth =
                            UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    roles.map { SimpleGrantedAuthority(it) }
                            )
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (_: Exception) {
                // ignore - unauthorized will be enforced downstream if needed
            }
        }
        filterChain.doFilter(request, response)
    }
}
