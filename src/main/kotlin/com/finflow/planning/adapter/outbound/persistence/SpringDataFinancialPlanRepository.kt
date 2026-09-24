package com.finflow.planning.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFinancialPlanRepository : JpaRepository<FinancialPlanEntity, UUID> {
    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialPlanEntity?

    fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlanEntity?
}
