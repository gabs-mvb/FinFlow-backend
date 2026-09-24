package com.finflow.profile.adapter.inbound.http

import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class UpsertFinancialProfileRequestBody(
    @field:Valid
    val monthlyIncome: MoneyInputBody,
    @field:Min(1)
    @field:Max(28)
    val payDay: Int,
    @field:Valid
    val essentialMonthlyExpenses: MoneyInputBody,
    @field:Valid
    val variableMonthlyBudget: MoneyInputBody,
    @field:Valid
    val minimumCashBuffer: MoneyInputBody,
    @field:Min(1)
    @field:Max(24)
    val emergencyTargetMonths: Int = 6,
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val reserveContributionRate: BigDecimal = BigDecimal("0.10"),
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    val riskProfile: RiskProfile = RiskProfile.CONSERVATIVE,
    val autopilotMode: AutopilotMode = AutopilotMode.OBSERVER,
)

fun UpsertFinancialProfileRequestBody.toCommand(): UpsertFinancialProfileRequest =
    UpsertFinancialProfileRequest(
        monthlyIncome = monthlyIncome.toCommand(),
        payDay = payDay,
        essentialMonthlyExpenses = essentialMonthlyExpenses.toCommand(),
        variableMonthlyBudget = variableMonthlyBudget.toCommand(),
        minimumCashBuffer = minimumCashBuffer.toCommand(),
        emergencyTargetMonths = emergencyTargetMonths,
        reserveContributionRate = reserveContributionRate,
        investmentContributionRate = investmentContributionRate,
        riskProfile = riskProfile,
        autopilotMode = autopilotMode,
    )
