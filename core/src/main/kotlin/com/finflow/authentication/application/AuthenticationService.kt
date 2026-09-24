package com.finflow.authentication.application

import com.finflow.authentication.application.model.LoginRequestDto
import com.finflow.authentication.application.model.LoginResponseDto
import com.finflow.authentication.application.model.RegisterRequestDto
import com.finflow.authentication.application.model.UserResponseDto
import com.finflow.authentication.application.port.inbound.AuthenticationUseCases
import com.finflow.authentication.application.port.outbound.CredentialAuthenticator
import com.finflow.authentication.application.port.outbound.PasswordHasher
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.AccountDeactivatedException
import com.finflow.shared.domain.RegistrationRejectedException
import com.finflow.shared.domain.ResourceNotFoundException

class AuthenticationService(
    private val currentUser: CurrentUser,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordHasher,
    private val authenticator: CredentialAuthenticator,
) : AuthenticationUseCases {
    override fun me(): UserResponseDto =
        currentUser.user().let {
            UserResponseDto(it.id, it.name, it.email, it.onboardingCompleted)
        }

    override fun login(request: LoginRequestDto): LoginResponseDto {
        val token = authenticator.authenticate(request.email.trim(), request.password)
        val user =
            userRepository
                .findByEmail(request.email.trim())
                .orElseThrow {
                    ResourceNotFoundException("User not found")
                }
        if (!user.active) {
            throw AccountDeactivatedException()
        }
        return LoginResponseDto(
            id = user.id,
            name = user.name,
            email = user.email,
            token = token,
            onboardingCompleted = user.onboardingCompleted,
        )
    }

    override fun register(request: RegisterRequestDto): UserResponseDto {
        if (request.password.isNullOrBlank()) {
            throw IllegalArgumentException("Password cannot be empty")
        }
        if (userRepository.existsByEmail(request.email.trim())) {
            throw RegistrationRejectedException("User already registered with this email")
        }
        require(request.password.toByteArray(Charsets.UTF_8).size <= 72) { "Password exceeds 72 bytes" }
        val encodedPassword: String = passwordEncoder.encode(request.password)
        val newUser =
            User(
                name = request.name.trim(),
                email = request.email.trim(),
                password = encodedPassword,
            )
        val savedUser = userRepository.save(newUser)
        return UserResponseDto(
            id = savedUser.id,
            name = savedUser.name,
            email = savedUser.email,
            onboardingCompleted = savedUser.onboardingCompleted,
        )
    }
}
