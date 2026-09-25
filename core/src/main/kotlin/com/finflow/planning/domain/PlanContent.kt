package com.finflow.planning.domain

import com.finflow.transaction.domain.TransactionCategory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PlanSource { RULE_BASED, AI, MANUAL }

data class CategoryBudget(
    val category: TransactionCategory,
    val amount: BigDecimal,
    val reason: String,
)

data class PlanActionDraft(
    val type: ActionType,
    val amount: BigDecimal,
    val riskLevel: RiskLevel,
    val rationale: String,
)

/** Recommendations can be replaced; observed balances and transactions stay at their source. */
data class PlanContent(
    val asOf: LocalDate,
    val nextIncomeDate: LocalDate,
    val summary: String,
    val analysis: String,
    val emergencyReserveTarget: BigDecimal,
    val remainingVariableBudget: BigDecimal,
    val minimumCashBuffer: BigDecimal,
    val debtPaymentRecommendation: BigDecimal,
    val reserveContribution: BigDecimal,
    val investmentContribution: BigDecimal,
    val dailySpendingLimit: BigDecimal,
    val categoryBudgets: List<CategoryBudget>,
    val allocations: List<PlannedAllocation>,
    val actions: List<PlanActionDraft>,
    val warnings: List<String>,
) {
    fun validate(
        operatingBalance: BigDecimal,
        committedObligations: BigDecimal,
    ) {
        require(summary.isNotBlank() && summary.length <= 2000) { "Resumo inválido" }
        require(analysis.isNotBlank() && analysis.length <= 16000) { "Análise inválida" }
        val days = ChronoUnit.DAYS.between(asOf, nextIncomeDate)
        require(days in 1..366) { "A próxima renda deve ocorrer entre 1 e 366 dias após a data do plano" }
        listOf(
            emergencyReserveTarget,
            remainingVariableBudget,
            minimumCashBuffer,
            debtPaymentRecommendation,
            reserveContribution,
            investmentContribution,
            dailySpendingLimit,
        ).forEach(::validateAmount)
        require(categoryBudgets.size <= 20 && categoryBudgets.map { it.category }.distinct().size == categoryBudgets.size) {
            "Categorias de orçamento duplicadas ou em excesso"
        }
        categoryBudgets.forEach {
            validateAmount(it.amount)
            require(it.reason.isNotBlank() && it.reason.length <= 500) { "Justificativa de categoria inválida" }
        }
        require(categoryBudgets.sumOf { it.amount } <= remainingVariableBudget) { "As categorias excedem o orçamento variável" }
        require(allocations.size <= 20 && allocations.map { it.assetClass }.distinct().size == allocations.size) {
            "Alocações duplicadas ou em excesso"
        }
        allocations.forEach { validateAmount(it.amount) }
        require(allocations.isEmpty() || allocations.sumOf { it.amount }.compareTo(investmentContribution) == 0) {
            "As alocações devem somar o aporte em investimentos"
        }
        val available = (operatingBalance - committedObligations - remainingVariableBudget - minimumCashBuffer).max(BigDecimal.ZERO)
        require(debtPaymentRecommendation + reserveContribution + investmentContribution <= available) {
            "As recomendações de aporte e dívida excedem o caixa disponível"
        }
        require(dailySpendingLimit.multiply(days.toBigDecimal()) <= freeBalance(operatingBalance, committedObligations)) {
            "O limite diário excede o saldo livre do período"
        }
        require(actions.size <= 20) { "O plano permite no máximo 20 ações" }
        actions.forEach {
            validateAmount(it.amount)
            require(it.rationale.isNotBlank() && it.rationale.length <= 500) { "Justificativa de ação inválida" }
        }
        val limits =
            mapOf(
                ActionType.RESERVE_FOR_OBLIGATIONS to committedObligations,
                ActionType.REDUCE_VARIABLE_SPENDING to shortfall(operatingBalance, committedObligations),
                ActionType.PAY_HIGH_COST_DEBT to debtPaymentRecommendation,
                ActionType.TRANSFER_TO_EMERGENCY_RESERVE to reserveContribution,
                ActionType.CREATE_INVESTMENT_CONTRIBUTION to investmentContribution,
                ActionType.CUSTOM to BigDecimal.ZERO,
            )
        actions.groupBy { it.type }.forEach { (type, items) ->
            require(items.sumOf { it.amount } <= limits.getValue(type)) { "As ações de $type excedem o valor planejado" }
        }
        require(warnings.size <= 10 && warnings.all { it.isNotBlank() && it.length <= 300 }) { "Avisos inválidos" }
    }

    fun freeBalance(
        operatingBalance: BigDecimal,
        committed: BigDecimal,
    ): BigDecimal =
        (
            operatingBalance - committed - remainingVariableBudget - minimumCashBuffer - debtPaymentRecommendation -
                reserveContribution - investmentContribution
        ).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN)

    fun shortfall(
        operatingBalance: BigDecimal,
        committed: BigDecimal,
    ): BigDecimal = (committed + remainingVariableBudget + minimumCashBuffer - operatingBalance).max(BigDecimal.ZERO)

    private fun validateAmount(value: BigDecimal) {
        require(value >= BigDecimal.ZERO && value.scale() <= 2 && value.precision() - value.scale() <= 17) {
            "Valor monetário inválido: use valores não negativos com até duas casas decimais"
        }
    }
}

data class PlanDetails(
    val source: PlanSource = PlanSource.RULE_BASED,
    val summary: String = "",
    val analysis: String = "",
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val model: String? = null,
    val promptVersion: String? = null,
)
