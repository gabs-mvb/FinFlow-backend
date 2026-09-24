package com.finflow.obligation.adapter.outbound.persistence

import com.finflow.obligation.domain.Obligation

internal fun ObligationEntity.toDomain(): Obligation =
    Obligation(
        userId = userId,
        id = id,
        name = name,
        obligationType = obligationType,
        amount = amount,
        currency = currency,
        dueDate = dueDate,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Obligation.toEntity(): ObligationEntity =
    ObligationEntity(
        userId = userId,
        id = id,
        name = name,
        obligationType = obligationType,
        amount = amount,
        currency = currency,
        dueDate = dueDate,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
