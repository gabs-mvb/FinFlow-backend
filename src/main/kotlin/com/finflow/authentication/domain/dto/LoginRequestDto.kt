package com.finflow.authentication.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequestDto(
    @field:NotBlank @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 72) val password: String
)
