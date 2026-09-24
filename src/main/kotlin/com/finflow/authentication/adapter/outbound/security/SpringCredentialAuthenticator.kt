package com.finflow.authentication.adapter.outbound.security

import com.finflow.authentication.application.port.outbound.CredentialAuthenticator
import com.finflow.authentication.application.port.outbound.PasswordHasher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringCredentialAuthenticator(
    private val manager: AuthenticationManager,
    private val tokens: JwtTokenProvider,
) : CredentialAuthenticator {
    override fun authenticate(
        email: String,
        password: String,
    ): String {
        val authentication = manager.authenticate(UsernamePasswordAuthenticationToken(email, password))
        SecurityContextHolder.getContext().authentication = authentication
        return tokens.generateToken(authentication)
    }
}

@Component
class BCryptPasswordHasher(
    private val encoder: PasswordEncoder,
) : PasswordHasher {
    override fun encode(password: String): String = requireNotNull(encoder.encode(password))
}
