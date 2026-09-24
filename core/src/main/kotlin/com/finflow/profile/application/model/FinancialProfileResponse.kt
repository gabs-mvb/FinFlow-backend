package com.finflow.profile.application.model

import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import com.finflow.shared.application.model.MoneyOutput
import java.math.BigDecimal
import java.time.OffsetDateTime

data class FinancialProfileResponse(
    val monthlyIncome: MoneyOutput,
    val payDay: Int,
    val essentialMonthlyExpenses: MoneyOutput,
    val variableMonthlyBudget: MoneyOutput,
    val minimumCashBuffer: MoneyOutput,
    val emergencyTargetMonths: Int,
    val reserveContributionRate: BigDecimal,
    val investmentContributionRate: BigDecimal,
    val riskProfile: RiskProfile,
    val autopilotMode: AutopilotMode,
    val updatedAt: OffsetDateTime,
)
