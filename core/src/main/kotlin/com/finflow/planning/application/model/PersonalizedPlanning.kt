package com.finflow.planning.application.model

import com.finflow.planning.domain.PlanContent
import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import com.finflow.transaction.domain.TransactionCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class PersonalizedPlanRequest(
    val asOf: LocalDate,
    val preferences: String = "",
) {
    init {
        require(preferences.length <= 2000) { "Preferências excedem 2000 caracteres" }
    }
}

data class ReplacePlanContentRequest(
    val expectedRevision: Int,
    val content: PlanContent,
) {
    init {
        require(expectedRevision >= 0) { "Revisão inválida" }
    }
}

data class PlanningProfile(
    val currency: String,
    val monthlyIncome: BigDecimal,
    val payDay: Int,
    val essentialMonthlyExpenses: BigDecimal,
    val variableMonthlyBudget: BigDecimal,
    val minimumCashBuffer: BigDecimal,
    val emergencyTargetMonths: Int,
    val reserveContributionRate: BigDecimal,
    val investmentContributionRate: BigDecimal,
    val riskProfile: RiskProfile,
    val autopilotMode: AutopilotMode,
)

data class SpendingBucket(
    val month: String,
    val category: TransactionCategory,
    val total: BigDecimal,
    val count: Int,
)

data class MonthlyCashFlow(
    val month: String,
    val income: BigDecimal,
    val expenses: BigDecimal,
    val investments: BigDecimal,
)

data class PlanningDebt(
    val type: String,
    val outstanding: BigDecimal,
    val monthlyPayment: BigDecimal,
    val annualRate: BigDecimal,
)

data class PlanningGoal(
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val targetDate: LocalDate?,
    val priority: Int,
)

data class PlanningObligation(
    val type: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
)

data class PlanningPosition(
    val assetClass: String,
    val currentValue: BigDecimal,
)

/** Explicit allowlist: no names, emails, account IDs, merchants or raw transaction descriptions. */
data class PlanningEvidence(
    val asOf: LocalDate,
    val historyFrom: LocalDate,
    val historyTo: LocalDate,
    val transactionCount: Int,
    val ignoredForeignCurrencyTransactions: Int,
    val profile: PlanningProfile,
    val operatingBalance: BigDecimal,
    val totalBalance: BigDecimal,
    val reserveBalance: BigDecimal,
    val activeConsent: Boolean,
    val cashFlow: List<MonthlyCashFlow>,
    val spendingByCategory: List<SpendingBucket>,
    val debts: List<PlanningDebt>,
    val goals: List<PlanningGoal>,
    val obligations: List<PlanningObligation>,
    val positions: List<PlanningPosition>,
    val baseline: PlanContent,
)

data class PersonalizationContext(
    val ownerId: Int,
    val evidence: PlanningEvidence,
)

data class PlanProposal(
    val content: PlanContent,
    val model: String,
    val promptVersion: String,
)

data class PlanRevision(
    val planId: UUID,
    val userId: Int,
    val revision: Int,
    val capturedAt: OffsetDateTime,
    val plan: FinancialPlanResponse,
)

data class PlanRevisionResponse(
    val revision: Int,
    val capturedAt: OffsetDateTime,
    val plan: FinancialPlanResponse,
)
