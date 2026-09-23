package com.finflow.shared.security

import com.finflow.authentication.security.jwt.JwtAuthenticationEntryPoint
import com.finflow.authentication.security.jwt.JwtAuthenticationFilter
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig {
    @Bean
    fun corsConfigurationSource(
        @Value("\${finflow.security.allowed-origins:http://localhost:3000}") allowedOrigins: String,
    ): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins.split(',').map(String::trim).filter(String::isNotBlank)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "Authorization", "Idempotency-Key")
            exposedHeaders = listOf("Content-Type")
            allowCredentials = false
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", configuration) }
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    ): SecurityFilterChain = http
        .cors { }
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/error", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling {
            it.authenticationEntryPoint(jwtAuthenticationEntryPoint)
        }
        .headers { headers ->
            headers.contentSecurityPolicy { policy -> policy.policyDirectives("default-src 'none'") }
        }
        .build()
}
