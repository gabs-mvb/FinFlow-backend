package com.finflow.account.adapter.outbound.persistence

import com.finflow.account.adapter.outbound.persistence.toDomain
import com.finflow.account.adapter.outbound.persistence.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFinancialAccountRepository : JpaRepository<FinancialAccountEntity, UUID> {
    fun existsByUserIdAndInstitutionIgnoreCaseAndExternalId(
        userId: Int,
        institution: String,
        externalId: String,
    ): Boolean

    fun findAllByUserId(userId: Int): List<FinancialAccountEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialAccountEntity?

    fun findAllByUserIdAndCurrency(
        userId: Int,
        currency: String,
    ): List<FinancialAccountEntity>
}
