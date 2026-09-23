package com.finflow.authentication.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 150, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false
)
