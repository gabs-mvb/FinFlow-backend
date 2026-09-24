package com.finflow.planning.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFinancialPlanRepository : JpaRepository<FinancialPlanEntity, UUID> {
    fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlanEntity?
}
