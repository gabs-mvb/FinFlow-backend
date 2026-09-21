package com.finflow.authentication.controller

import com.finflow.authentication.domain.dto.LoginRequestDto
import com.finflow.authentication.domain.dto.LoginResponseDto
import com.finflow.authentication.domain.dto.RegisterRequestDto
import com.finflow.authentication.domain.dto.UserResponseDto
import com.finflow.authentication.service.AuthenticationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        return ResponseEntity.ok(authenticationService.login(loginRequest))
    }

    @PostMapping("/register")
    fun register(@RequestBody registerRequest: RegisterRequestDto): ResponseEntity<UserResponseDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authenticationService.register(registerRequest))
    }
}
