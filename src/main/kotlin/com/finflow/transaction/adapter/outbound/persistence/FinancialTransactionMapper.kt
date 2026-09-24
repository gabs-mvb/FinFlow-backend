package com.finflow.transaction.adapter.outbound.persistence

import com.finflow.account.adapter.outbound.persistence.toDomain
import com.finflow.account.adapter.outbound.persistence.toEntity
import com.finflow.transaction.domain.FinancialTransaction

internal fun FinancialTransactionEntity.toDomain(): FinancialTransaction =
    FinancialTransaction(
        id = id,
        account = account.toDomain(),
        externalId = externalId,
        transactionType = transactionType,
        amount = amount,
        currency = currency,
        description = description,
        merchant = merchant,
        category = category,
        categorizationSource = categorizationSource,
        occurredAt = occurredAt,
        importedAt = importedAt,
    )

internal fun FinancialTransaction.toEntity(): FinancialTransactionEntity =
    FinancialTransactionEntity(
        id = id,
        account = account.toEntity(),
        externalId = externalId,
        transactionType = transactionType,
        amount = amount,
        currency = currency,
        description = description,
        merchant = merchant,
        category = category,
        categorizationSource = categorizationSource,
        occurredAt = occurredAt,
        importedAt = importedAt,
    )
