package com.finflow.goal.adapter.outbound.persistence

import com.finflow.goal.domain.Goal

internal fun GoalEntity.toDomain(): Goal =
    Goal(
        userId = userId,
        id = id,
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        currency = currency,
        targetDate = targetDate,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Goal.toEntity(): GoalEntity =
    GoalEntity(
        userId = userId,
        id = id,
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        currency = currency,
        targetDate = targetDate,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
