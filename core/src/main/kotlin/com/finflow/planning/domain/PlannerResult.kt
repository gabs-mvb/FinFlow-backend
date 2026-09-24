package com.finflow.planning.domain

import java.math.BigDecimal
import java.time.LocalDate

data class PlannerResult(
    val nextIncomeDate: LocalDate,
    val remainingVariableBudget: BigDecimal,
    val emergencyReserveTarget: BigDecimal,
    val projectedShortfall: BigDecimal,
    val debtPaymentRecommendation: BigDecimal,
    val reserveContribution: BigDecimal,
    val investmentContribution: BigDecimal,
    val freeRealBalance: BigDecimal,
    val dailySpendingLimit: BigDecimal,
    val warnings: List<String>,
)
