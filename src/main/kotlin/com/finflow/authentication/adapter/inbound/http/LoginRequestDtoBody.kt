package com.finflow.authentication.adapter.inbound.http

import com.finflow.authentication.application.model.LoginRequestDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequestDtoBody(
    @field:NotBlank @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 72) val password: String,
)

fun LoginRequestDtoBody.toCommand(): LoginRequestDto =
    LoginRequestDto(
        email = email,
        password = password,
    )
