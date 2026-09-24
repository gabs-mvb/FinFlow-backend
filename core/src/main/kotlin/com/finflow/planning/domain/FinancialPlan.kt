package com.finflow.planning.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialPlan(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val asOf: LocalDate = LocalDate.now(),
    val nextIncomeDate: LocalDate = LocalDate.now(),
    val currency: String = "BRL",
    val operatingBalance: BigDecimal = BigDecimal.ZERO,
    val committedObligations: BigDecimal = BigDecimal.ZERO,
    val remainingVariableBudget: BigDecimal = BigDecimal.ZERO,
    val minimumCashBuffer: BigDecimal = BigDecimal.ZERO,
    val debtPaymentRecommendation: BigDecimal = BigDecimal.ZERO,
    val reserveContribution: BigDecimal = BigDecimal.ZERO,
    val investmentContribution: BigDecimal = BigDecimal.ZERO,
    val freeRealBalance: BigDecimal = BigDecimal.ZERO,
    val projectedShortfall: BigDecimal = BigDecimal.ZERO,
    val dailySpendingLimit: BigDecimal = BigDecimal.ZERO,
    val warnings: List<String> = emptyList(),
    val allocations: List<PlannedAllocation> = emptyList(),
    val generatedAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val revision: Int = 0,
    val details: PlanDetails = PlanDetails(),
    val totalBalanceSnapshot: BigDecimal? = null,
    val reserveBalanceSnapshot: BigDecimal? = null,
    val reserveTargetSnapshot: BigDecimal? = null,
)
