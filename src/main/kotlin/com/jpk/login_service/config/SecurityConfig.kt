package com.jpk.login_service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
                .csrf { it.disable() }
                .authorizeHttpRequests { auth ->
                    auth.requestMatchers("/api/auth/**", "/api/users/**")
                            .permitAll()
                            .anyRequest()
                            .permitAll() // allow all for now; tighten later
                }
                .httpBasic { it.disable() }
                .formLogin { it.disable() }
        return http.build()
    }
}
