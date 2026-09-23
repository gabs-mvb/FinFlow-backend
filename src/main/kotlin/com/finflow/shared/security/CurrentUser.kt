package com.finflow.shared.security

import com.finflow.authentication.domain.User
import com.finflow.authentication.domain.UserRepository
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Identity always comes from verified authentication, never a request userId. */
@Component
class CurrentUser(private val users: UserRepository) {
    fun user(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) throw AuthenticationCredentialsNotFoundException("Autenticação obrigatória")
        return users.findByEmail(authentication.name)
            .filter { it.active }
            .orElseThrow { AuthenticationCredentialsNotFoundException("Usuário indisponível") }
    }

    fun id(): Int = user().id
}
