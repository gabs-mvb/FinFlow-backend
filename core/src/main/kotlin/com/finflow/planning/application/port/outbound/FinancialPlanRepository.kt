package com.finflow.planning.application.port.outbound

import com.finflow.planning.domain.FinancialPlan
import java.util.UUID

interface FinancialPlanRepository {
    /** Serializes recalculation and review for one user within the surrounding transaction. */
    fun lockChangesForUser(userId: Int)

    fun save(value: FinancialPlan): FinancialPlan

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialPlan?

    fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlan?
}
