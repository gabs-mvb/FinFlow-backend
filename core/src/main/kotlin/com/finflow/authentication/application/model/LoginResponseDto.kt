package com.finflow.authentication.application.model

data class LoginResponseDto(
    val id: Int,
    val name: String,
    val email: String,
    val token: String,
    val onboardingCompleted: Boolean = false,
)
