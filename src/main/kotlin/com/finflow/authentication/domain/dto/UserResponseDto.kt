package com.finflow.authentication.domain.dto

data class UserResponseDto(
    val id: Int,
    val name: String,
    val email: String,
    val onboardingCompleted: Boolean = false
)
