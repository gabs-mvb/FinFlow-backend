package com.finflow.authentication.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequestDto(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:NotBlank @field:Email @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 72) val password: String
)
