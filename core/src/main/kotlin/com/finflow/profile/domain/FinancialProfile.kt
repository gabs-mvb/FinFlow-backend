package com.finflow.profile.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialProfile(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val currency: String = "BRL",
    val monthlyIncome: BigDecimal = BigDecimal.ZERO,
    val payDay: Int = 1,
    val essentialMonthlyExpenses: BigDecimal = BigDecimal.ZERO,
    val variableMonthlyBudget: BigDecimal = BigDecimal.ZERO,
    val minimumCashBuffer: BigDecimal = BigDecimal.ZERO,
    val emergencyTargetMonths: Int = 6,
    val reserveContributionRate: BigDecimal = BigDecimal("0.10"),
    val investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    val riskProfile: RiskProfile = RiskProfile.CONSERVATIVE,
    val autopilotMode: AutopilotMode = AutopilotMode.OBSERVER,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
