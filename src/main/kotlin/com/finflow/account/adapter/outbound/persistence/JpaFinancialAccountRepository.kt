package com.finflow.account.adapter.outbound.persistence

import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.FinancialAccount
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaFinancialAccountRepository(
    private val delegate: SpringDataFinancialAccountRepository,
) : FinancialAccountRepository {
    override fun save(value: FinancialAccount): FinancialAccount = delegate.save(value.toEntity()).toDomain()
    override fun deleteByIdAndUserId(id: UUID, userId: Int) { delegate.deleteByIdAndUserId(id, userId) }

    override fun existsByUserIdAndInstitutionIgnoreCaseAndExternalId(
        userId: Int,
        institution: String,
        externalId: String,
    ): Boolean = delegate.existsByUserIdAndInstitutionIgnoreCaseAndExternalId(userId, institution, externalId)

    override fun findAllByUserId(userId: Int): List<FinancialAccount> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialAccount? = delegate.findByIdAndUserId(id, userId)?.toDomain()

    override fun findAllByUserIdAndCurrency(
        userId: Int,
        currency: String,
    ): List<FinancialAccount> =
        delegate.findAllByUserIdAndCurrency(userId, currency).map {
            it.toDomain()
        }

    override fun adjustTrackedBalance(
        id: UUID,
        userId: Int,
        delta: BigDecimal,
        syncedAt: OffsetDateTime,
    ): FinancialAccount {
        check(delegate.adjustTrackedBalance(id, userId, delta, syncedAt) == 1) { "Conta não encontrada para ajuste" }
        return checkNotNull(delegate.findByIdAndUserId(id, userId)) { "Conta não encontrada após ajuste" }.toDomain()
    }
}
