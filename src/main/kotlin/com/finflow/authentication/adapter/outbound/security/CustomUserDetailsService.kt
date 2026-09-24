package com.finflow.authentication.adapter.outbound.security

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.security.core.userdetails.User as SpringUser

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user =
            userRepository
                .findByEmail(email)
                .orElseThrow { UsernameNotFoundException("User not found with email: $email") }

        return SpringUser
            .withUsername(user.email)
            .password(user.password)
            .authorities(listOf(SimpleGrantedAuthority("ROLE_USER")))
            .disabled(!user.active)
            .build()
    }
}
