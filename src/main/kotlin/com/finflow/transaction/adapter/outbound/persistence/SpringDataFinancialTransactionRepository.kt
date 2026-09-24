package com.finflow.transaction.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface SpringDataFinancialTransactionRepository : JpaRepository<FinancialTransactionEntity, UUID> {
    fun existsByAccountIdAndExternalId(
        accountId: UUID,
        externalId: String,
    ): Boolean

    fun findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        userId: Int,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<FinancialTransactionEntity>
}
