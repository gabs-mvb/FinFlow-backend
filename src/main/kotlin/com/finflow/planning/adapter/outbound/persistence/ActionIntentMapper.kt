package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.domain.ActionIntent

internal fun ActionIntentEntity.toDomain(): ActionIntent =
    ActionIntent(
        id = id,
        plan = plan.toDomain(),
        actionType = actionType,
        amount = amount,
        currency = currency,
        riskLevel = riskLevel,
        requiresApproval = requiresApproval,
        status = status,
        rationale = rationale,
        createdAt = createdAt,
        reviewedAt = reviewedAt,
    )

internal fun ActionIntent.toEntity(): ActionIntentEntity =
    ActionIntentEntity(
        id = id,
        plan = plan.toEntity(),
        actionType = actionType,
        amount = amount,
        currency = currency,
        riskLevel = riskLevel,
        requiresApproval = requiresApproval,
        status = status,
        rationale = rationale,
        createdAt = createdAt,
        reviewedAt = reviewedAt,
    )
