package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.domain.FinancialPlan
import org.springframework.stereotype.Repository

@Repository
class JpaFinancialPlanRepository(
    private val delegate: SpringDataFinancialPlanRepository,
) : FinancialPlanRepository {
    override fun save(value: FinancialPlan): FinancialPlan = delegate.save(value.toEntity()).toDomain()

    override fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlan? =
        delegate.findFirstByUserIdOrderByGeneratedAtDesc(userId)?.toDomain()
}
