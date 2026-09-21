package com.finflow.shared.security

import jakarta.servlet.FilterChain
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

private const val API_KEY_HEADER = "X-API-Key"

@Configuration
class ApiKeySecurityConfig {
    @Bean
    fun corsConfigurationSource(
        @Value("\${finflow.security.allowed-origins:http://localhost:3000}") allowedOrigins: String,
    ): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins.split(',').map(String::trim).filter(String::isNotBlank)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "OPTIONS")
            allowedHeaders = listOf("Content-Type", API_KEY_HEADER, "Idempotency-Key")
            exposedHeaders = listOf("Content-Type")
            allowCredentials = false
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", configuration) }
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
    ): SecurityFilterChain = http
        .cors { }
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/error", "/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ ->
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/problem+json"
                response.writer.write(
                    """{"title":"UNAUTHORIZED","status":401,"detail":"Chave de API ausente ou inválida"}""",
                )
            }
        }
        .headers { headers ->
            headers.contentSecurityPolicy { policy -> policy.policyDirectives("default-src 'none'") }
        }
        .build()
}

@Component
class ApiKeyAuthenticationFilter(
    @Value("\${finflow.security.api-key}") private val expectedApiKey: String,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/actuator/health") || request.requestURI == "/actuator/info"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val provided = request.getHeader(API_KEY_HEADER)
        if (provided != null && constantTimeEquals(provided, expectedApiKey)) {
            val authentication = UsernamePasswordAuthenticationToken(
                "owner",
                null,
                listOf(SimpleGrantedAuthority("ROLE_OWNER")),
            )
            SecurityContextHolder.getContext().authentication = authentication
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store")
        filterChain.doFilter(request, response)
    }

    private fun constantTimeEquals(provided: String, expected: String): Boolean =
        MessageDigest.isEqual(
            provided.toByteArray(Charsets.UTF_8),
            expected.toByteArray(Charsets.UTF_8),
        )
}
