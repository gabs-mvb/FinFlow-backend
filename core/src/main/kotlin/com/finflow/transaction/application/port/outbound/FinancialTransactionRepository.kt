package com.finflow.transaction.application.port.outbound

import com.finflow.transaction.domain.FinancialTransaction
import java.time.OffsetDateTime
import java.util.UUID

interface FinancialTransactionRepository {
    fun save(value: FinancialTransaction): FinancialTransaction

    fun saveAll(values: List<FinancialTransaction>): List<FinancialTransaction>

    fun existsByAccountIdAndExternalId(
        accountId: UUID,
        externalId: String,
    ): Boolean

    fun findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        userId: Int,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<FinancialTransaction>
}
