package com.finflow.planning.application.port.outbound

import com.finflow.planning.domain.FinancialPlan

interface FinancialPlanRepository {
    fun save(value: FinancialPlan): FinancialPlan

    fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlan?
}
