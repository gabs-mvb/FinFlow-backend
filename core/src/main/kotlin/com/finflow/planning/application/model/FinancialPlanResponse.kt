package com.finflow.planning.application.model

import com.finflow.planning.domain.PlanContent
import com.finflow.planning.domain.PlanDetails
import com.finflow.portfolio.application.model.ContributionAllocation
import com.finflow.shared.application.model.MoneyOutput
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialPlanResponse(
    val id: UUID,
    val asOf: LocalDate,
    val nextIncomeDate: LocalDate,
    val totalConsolidatedBalance: MoneyOutput,
    val operatingBalance: MoneyOutput,
    val emergencyReserveBalance: MoneyOutput,
    val emergencyReserveTarget: MoneyOutput,
    val committedObligations: MoneyOutput,
    val remainingVariableBudget: MoneyOutput,
    val minimumCashBuffer: MoneyOutput,
    val debtPaymentRecommendation: MoneyOutput,
    val reserveContribution: MoneyOutput,
    val investmentContribution: MoneyOutput,
    val freeRealBalance: MoneyOutput,
    val projectedShortfall: MoneyOutput,
    val dailySpendingLimit: MoneyOutput,
    val contributionAllocation: List<ContributionAllocation>,
    val actions: List<ActionIntentResponse>,
    val warnings: List<String>,
    val generatedAt: OffsetDateTime,
    val revision: Int = 0,
    val updatedAt: OffsetDateTime? = null,
    val details: PlanDetails = PlanDetails(),
    val content: PlanContent? = null,
)
