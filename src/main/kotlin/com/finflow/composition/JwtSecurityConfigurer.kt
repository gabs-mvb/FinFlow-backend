package com.finflow.composition

import com.finflow.authentication.adapter.inbound.security.JwtAuthenticationEntryPoint
import com.finflow.authentication.adapter.inbound.security.JwtAuthenticationFilter
import com.finflow.authentication.adapter.outbound.security.CustomUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@EnableMethodSecurity
@Component
class JwtSecurityConfigurer(
    private val customUserDetailsService: CustomUserDetailsService,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager
}
