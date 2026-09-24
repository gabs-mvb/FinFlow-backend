package com.finflow.authentication.domain

import java.time.LocalDateTime

data class User(
    val id: Int = 0,
    val name: String,
    val email: String,
    val password: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val active: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
