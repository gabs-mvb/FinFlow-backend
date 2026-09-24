package com.finflow.account.application.port.outbound

import com.finflow.account.domain.FinancialAccount
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
}
