package com.finflow.account.application.port.outbound

import com.finflow.account.domain.FinancialAccount
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

interface FinancialAccountRepository {
    fun save(value: FinancialAccount): FinancialAccount

    fun existsByUserIdAndInstitutionIgnoreCaseAndExternalId(
        userId: Int,
        institution: String,
        externalId: String,
    ): Boolean

    fun findAllByUserId(userId: Int): List<FinancialAccount>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialAccount?

    fun findAllByUserIdAndCurrency(
        userId: Int,
        currency: String,
    ): List<FinancialAccount>

    /** Atomically adjusts the FinFlow-tracked balance for imported events. */
    fun adjustTrackedBalance(
        id: UUID,
        userId: Int,
        delta: BigDecimal,
        syncedAt: OffsetDateTime,
    ): FinancialAccount
}
