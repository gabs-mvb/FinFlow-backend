package com.finflow.profile.application.model

import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import com.finflow.shared.application.model.MoneyInput
import java.math.BigDecimal

data class UpsertFinancialProfileRequest(
    val monthlyIncome: MoneyInput,
    val payDay: Int,
    val essentialMonthlyExpenses: MoneyInput,
    val variableMonthlyBudget: MoneyInput,
    val minimumCashBuffer: MoneyInput,
    val emergencyTargetMonths: Int = 6,
    val reserveContributionRate: BigDecimal = BigDecimal("0.10"),
    val investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    val riskProfile: RiskProfile = RiskProfile.CONSERVATIVE,
    val autopilotMode: AutopilotMode = AutopilotMode.OBSERVER,
) {
    init {
        require(payDay >= 1) { "Valor inválido para payDay" }
        require(payDay <= 28) { "Valor inválido para payDay" }
        require(emergencyTargetMonths >= 1) { "Valor inválido para emergencyTargetMonths" }
        require(emergencyTargetMonths <= 24) { "Valor inválido para emergencyTargetMonths" }
        require(reserveContributionRate >= java.math.BigDecimal("0.0")) { "Valor inválido para reserveContributionRate" }
        require(reserveContributionRate <= java.math.BigDecimal("1.0")) { "Valor inválido para reserveContributionRate" }
        require(investmentContributionRate >= java.math.BigDecimal("0.0")) { "Valor inválido para investmentContributionRate" }
        require(investmentContributionRate <= java.math.BigDecimal("1.0")) { "Valor inválido para investmentContributionRate" }
    }
}
