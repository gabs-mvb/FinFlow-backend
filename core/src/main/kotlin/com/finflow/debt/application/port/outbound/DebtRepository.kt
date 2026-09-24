package com.finflow.debt.application.port.outbound

import com.finflow.debt.domain.Debt
import com.finflow.debt.domain.DebtStatus
import java.util.UUID

interface DebtRepository {
    fun save(value: Debt): Debt

    fun findAllByUserId(userId: Int): List<Debt>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Debt?

    fun findAllByUserIdAndStatus(
        userId: Int,
        status: DebtStatus,
    ): List<Debt>
}
