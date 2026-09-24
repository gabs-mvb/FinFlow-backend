package com.finflow.debt.adapter.outbound.persistence

import com.finflow.debt.domain.Debt

internal fun DebtEntity.toDomain(): Debt =
    Debt(
        userId = userId,
        id = id,
        name = name,
        debtType = debtType,
        outstandingAmount = outstandingAmount,
        monthlyPayment = monthlyPayment,
        currency = currency,
        annualEffectiveRate = annualEffectiveRate,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Debt.toEntity(): DebtEntity =
    DebtEntity(
        userId = userId,
        id = id,
        name = name,
        debtType = debtType,
        outstandingAmount = outstandingAmount,
        monthlyPayment = monthlyPayment,
        currency = currency,
        annualEffectiveRate = annualEffectiveRate,
        priority = priority,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
