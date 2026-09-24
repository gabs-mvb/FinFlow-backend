package com.finflow.account.adapter.outbound.persistence

import com.finflow.account.adapter.outbound.persistence.toDomain
import com.finflow.account.adapter.outbound.persistence.toEntity
import com.finflow.account.domain.FinancialAccount

internal fun FinancialAccountEntity.toDomain(): FinancialAccount =
    FinancialAccount(
        userId = userId,
        id = id,
        institution = institution,
        externalId = externalId,
        name = name,
        accountType = accountType,
        purpose = purpose,
        availableBalance = availableBalance,
        currency = currency,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun FinancialAccount.toEntity(): FinancialAccountEntity =
    FinancialAccountEntity(
        userId = userId,
        id = id,
        institution = institution,
        externalId = externalId,
        name = name,
        accountType = accountType,
        purpose = purpose,
        availableBalance = availableBalance,
        currency = currency,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
