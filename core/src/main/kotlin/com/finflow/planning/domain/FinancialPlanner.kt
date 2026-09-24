package com.finflow.planning.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object FinancialPlanner {
    /**
     * Distribui apenas o caixa disponível até a próxima renda. A ordem protege
     * obrigações e caixa mínimo antes de dívida cara, reserva e investimento.
     */
    fun calculate(input: PlannerInput): PlannerResult {
        require(input.payDay in 1..28) { "Dia de recebimento inválido" }
        require(input.operatingBalance >= BigDecimal.ZERO) { "Saldo operacional não pode ser negativo" }
        val nextIncomeDate = nextIncomeDate(input.asOf, input.payDay)
        val remainingVariableBudget =
            (input.variableMonthlyBudget - input.variableSpentThisMonth)
                .max(BigDecimal.ZERO)
        val emergencyTarget =
            input.essentialMonthlyExpenses
                .multiply(input.emergencyTargetMonths.toBigDecimal())
        val protectedAmount = input.committedObligations + remainingVariableBudget + input.minimumCashBuffer
        val beforePriorities = input.operatingBalance - protectedAmount
        val projectedShortfall = (-beforePriorities).max(BigDecimal.ZERO)
        var available = beforePriorities.max(BigDecimal.ZERO)
        val debtPayment =
            if (input.highCostDebtOutstanding > BigDecimal.ZERO) {
                available.min(input.highCostDebtOutstanding)
            } else {
                BigDecimal.ZERO
            }
        available -= debtPayment
        val reserveGap = (emergencyTarget - input.emergencyReserveBalance).max(BigDecimal.ZERO)
        val reserveMonthlyCap = input.monthlyIncome.multiply(input.reserveContributionRate)
        val reserveContribution = available.min(reserveGap).min(reserveMonthlyCap)
        available -= reserveContribution
        val reserveWillBeComplete = input.emergencyReserveBalance + reserveContribution >= emergencyTarget
        val investmentMonthlyCap = input.monthlyIncome.multiply(input.investmentContributionRate)
        val investmentContribution =
            if (
                reserveWillBeComplete && input.highCostDebtOutstanding - debtPayment <= BigDecimal.ZERO
            ) {
                available.min(investmentMonthlyCap)
            } else {
                BigDecimal.ZERO
            }
        available -= investmentContribution
        val freeRealBalance = available.max(BigDecimal.ZERO)
        val days = ChronoUnit.DAYS.between(input.asOf, nextIncomeDate).coerceAtLeast(1)
        val dailyLimit = freeRealBalance.divide(days.toBigDecimal(), 2, RoundingMode.DOWN)
        val warnings =
            buildList {
                if (projectedShortfall > BigDecimal.ZERO) {
                    add("O saldo operacional projetado não cobre todos os compromissos antes da próxima renda")
                }
                if (input.highCostDebtOutstanding > BigDecimal.ZERO) {
                    add("Dívida de alto custo tem prioridade sobre novos investimentos")
                }
                if (reserveGap > BigDecimal.ZERO) {
                    add("A reserva de emergência está abaixo da meta configurada")
                }
            }
        return PlannerResult(
            nextIncomeDate = nextIncomeDate,
            remainingVariableBudget = remainingVariableBudget.moneyScale(),
            emergencyReserveTarget = emergencyTarget.moneyScale(),
            projectedShortfall = projectedShortfall.moneyScale(),
            debtPaymentRecommendation = debtPayment.moneyScale(),
            reserveContribution = reserveContribution.moneyScale(),
            investmentContribution = investmentContribution.moneyScale(),
            freeRealBalance = freeRealBalance.moneyScale(),
            dailySpendingLimit = dailyLimit.moneyScale(),
            warnings = warnings,
        )
    }

    private fun nextIncomeDate(
        asOf: LocalDate,
        payDay: Int,
    ): LocalDate =
        if (asOf.dayOfMonth < payDay) {
            asOf.withDayOfMonth(payDay)
        } else {
            asOf.plusMonths(1).withDayOfMonth(payDay)
        }
}

private fun BigDecimal.moneyScale(): BigDecimal = setScale(2, RoundingMode.HALF_EVEN)
