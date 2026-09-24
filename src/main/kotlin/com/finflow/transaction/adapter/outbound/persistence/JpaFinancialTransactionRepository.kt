package com.finflow.transaction.adapter.outbound.persistence

import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.domain.FinancialTransaction
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class JpaFinancialTransactionRepository(
    private val delegate: SpringDataFinancialTransactionRepository,
) : FinancialTransactionRepository {
    override fun save(value: FinancialTransaction): FinancialTransaction = delegate.save(value.toEntity()).toDomain()

    override fun saveAll(values: List<FinancialTransaction>): List<FinancialTransaction> =
        delegate
            .saveAll(
                values.map {
                    it.toEntity()
                },
            ).map { it.toDomain() }

    override fun existsByAccountIdAndExternalId(
        accountId: UUID,
        externalId: String,
    ): Boolean = delegate.existsByAccountIdAndExternalId(accountId, externalId)

    override fun findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        userId: Int,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<FinancialTransaction> =
        delegate.findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(userId, from, to).map {
            it.toDomain()
        }
}
