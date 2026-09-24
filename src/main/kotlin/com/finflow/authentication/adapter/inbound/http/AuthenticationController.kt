package com.finflow.authentication.adapter.inbound.http

import com.finflow.authentication.application.model.LoginResponseDto
import com.finflow.authentication.application.model.UserResponseDto
import com.finflow.authentication.application.port.inbound.AuthenticationUseCases
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationUseCases,
) {
    @GetMapping("/me")
    fun me(): UserResponseDto = authenticationService.me()

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequestDtoBody,
    ): ResponseEntity<LoginResponseDto> = ResponseEntity.ok(authenticationService.login(loginRequest.toCommand()))

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody registerRequest: RegisterRequestDtoBody,
    ): ResponseEntity<UserResponseDto> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(authenticationService.register(registerRequest.toCommand()))
}
