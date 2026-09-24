package com.finflow.authentication.application.port.inbound

import com.finflow.authentication.application.model.LoginRequestDto
import com.finflow.authentication.application.model.LoginResponseDto
import com.finflow.authentication.application.model.RegisterRequestDto
import com.finflow.authentication.application.model.UserResponseDto

interface AuthenticationUseCases {
    fun me(): UserResponseDto

    fun login(request: LoginRequestDto): LoginResponseDto

    fun register(request: RegisterRequestDto): UserResponseDto
}
