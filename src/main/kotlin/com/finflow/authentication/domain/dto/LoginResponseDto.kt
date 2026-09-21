package com.finflow.authentication.domain.dto

data class LoginResponseDto(
    val id: Long,
    val name: String,
    val email: String,
    val token: String
)
