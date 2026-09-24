package com.finflow.planning.domain

import java.math.BigDecimal
import java.time.LocalDate

data class PlannerInput(
    val asOf: LocalDate,
    val payDay: Int,
    val monthlyIncome: BigDecimal,
    val essentialMonthlyExpenses: BigDecimal,
    val variableMonthlyBudget: BigDecimal,
    val variableSpentThisMonth: BigDecimal,
    val minimumCashBuffer: BigDecimal,
    val emergencyTargetMonths: Int,
    val reserveContributionRate: BigDecimal,
    val investmentContributionRate: BigDecimal,
    val operatingBalance: BigDecimal,
    val emergencyReserveBalance: BigDecimal,
    val committedObligations: BigDecimal,
    val highCostDebtOutstanding: BigDecimal,
)
