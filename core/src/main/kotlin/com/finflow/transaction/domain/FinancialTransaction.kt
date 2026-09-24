package com.finflow.transaction.domain

import com.finflow.account.domain.FinancialAccount
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialTransaction(
    val id: UUID = UUID.randomUUID(),
    val account: FinancialAccount = FinancialAccount(),
    val externalId: String = "",
    val transactionType: TransactionType = TransactionType.DEBIT,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val description: String = "",
    val merchant: String? = null,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val categorizationSource: CategorizationSource = CategorizationSource.RULE,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
    val importedAt: OffsetDateTime = OffsetDateTime.now(),
)
