package com.finflow.debt.adapter.outbound.persistence

import com.finflow.debt.domain.DebtStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataDebtRepository : JpaRepository<DebtEntity, UUID> {
    fun findAllByUserId(userId: Int): List<DebtEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): DebtEntity?

    fun findAllByUserIdAndStatus(
        userId: Int,
        status: DebtStatus,
    ): List<DebtEntity>
}
