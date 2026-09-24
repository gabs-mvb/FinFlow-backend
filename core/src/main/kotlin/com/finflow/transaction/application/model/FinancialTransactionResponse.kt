package com.finflow.transaction.application.model

import com.finflow.shared.application.model.MoneyOutput
import com.finflow.transaction.domain.CategorizationSource
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialTransactionResponse(
    val id: UUID,
    val accountId: UUID,
    val externalId: String,
    val type: TransactionType,
    val amount: MoneyOutput,
    val description: String,
    val merchant: String?,
    val category: TransactionCategory,
    val categorizationSource: CategorizationSource,
    val occurredAt: OffsetDateTime,
)
