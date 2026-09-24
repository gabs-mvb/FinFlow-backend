package com.finflow.authentication.adapter.outbound.persistence

import com.finflow.authentication.domain.User

internal fun UserEntity.toDomain(): User =
    User(
        id = id,
        name = name,
        email = email,
        password = password,
        createdAt = createdAt,
        active = active,
        onboardingCompleted = onboardingCompleted,
    )

internal fun User.toEntity(): UserEntity =
    UserEntity(
        id = id,
        name = name,
        email = email,
        password = password,
        createdAt = createdAt,
        active = active,
        onboardingCompleted = onboardingCompleted,
    )
