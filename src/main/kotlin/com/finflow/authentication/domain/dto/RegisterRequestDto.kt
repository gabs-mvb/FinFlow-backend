package com.finflow.authentication.domain.dto

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)
