package com.finflow.authentication.application.model

data class UserResponseDto(
    val id: Int,
    val name: String,
    val email: String,
    val onboardingCompleted: Boolean = false,
)
