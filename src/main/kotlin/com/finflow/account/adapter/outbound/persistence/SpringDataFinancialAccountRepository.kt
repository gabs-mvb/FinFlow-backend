package com.finflow.account.adapter.outbound.persistence

import com.finflow.account.adapter.outbound.persistence.toDomain
import com.finflow.account.adapter.outbound.persistence.toEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

interface SpringDataFinancialAccountRepository : JpaRepository<FinancialAccountEntity, UUID> {
    fun deleteByIdAndUserId(id: UUID, userId: Int): Long
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update FinancialAccountEntity account
        set account.availableBalance = account.availableBalance + :delta,
            account.lastSyncedAt = :syncedAt,
            account.updatedAt = :syncedAt
        where account.id = :id and account.userId = :userId
        """,
    )
    fun adjustTrackedBalance(
        @Param("id") id: UUID,
        @Param("userId") userId: Int,
        @Param("delta") delta: BigDecimal,
        @Param("syncedAt") syncedAt: OffsetDateTime,
    ): Int
}
