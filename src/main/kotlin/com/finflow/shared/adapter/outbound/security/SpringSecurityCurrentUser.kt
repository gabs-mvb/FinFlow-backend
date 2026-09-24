package com.finflow.shared.adapter.outbound.security

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.shared.application.port.outbound.CurrentUser
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SpringSecurityCurrentUser(
    private val users: UserRepository,
) : CurrentUser {
    override fun user(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) {
            throw AuthenticationCredentialsNotFoundException("Autenticação obrigatória")
        }
        return users
            .findByEmail(authentication.name)
            .filter { it.active }
            .orElseThrow { AuthenticationCredentialsNotFoundException("Usuário indisponível") }
    }

    override fun id(): Int = user().id
}
