package com.finflow.authentication.service

import com.finflow.authentication.domain.User
import com.finflow.authentication.domain.UserRepository
import com.finflow.authentication.domain.dto.LoginRequestDto
import com.finflow.authentication.domain.dto.LoginResponseDto
import com.finflow.authentication.domain.dto.RegisterRequestDto
import com.finflow.authentication.domain.dto.UserResponseDto
import com.finflow.authentication.security.jwt.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {

    fun login(request: LoginRequestDto): LoginResponseDto {
        val credentials = UsernamePasswordAuthenticationToken(
            request.email,
            request.password
        )

        val authentication = authenticationManager.authenticate(credentials)

        val user = userRepository.findByEmail(request.email)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            }

        if (!user.active) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "User account is deactivated")
        }

        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenProvider.generateToken(authentication)

        return LoginResponseDto(
            id = user.id,
            name = user.name,
            email = user.email,
            token = token
        )
    }

    fun register(request: RegisterRequestDto): UserResponseDto {
        if (request.password.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty")
        }

        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "User already registered with this email"
            )
        }

        val encodedPassword: String = passwordEncoder.encode(request.password)!!
        val newUser = User(
            name = request.name,
            email = request.email,
            password = encodedPassword
        )

        val savedUser = userRepository.save(newUser)

        return UserResponseDto(
            id = savedUser.id,
            name = savedUser.name,
            email = savedUser.email
        )
    }
}
