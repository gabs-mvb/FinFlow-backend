package com.finflow.planning

import com.finflow.planning.domain.FinancialPlanner
import com.finflow.planning.domain.PlannerInput
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialPlannerTest {
    @Test
    fun `protects obligations and variable budget before exposing free balance`() {
        val result =
            FinancialPlanner.calculate(
                baseInput(
                    operatingBalance = BigDecimal("9400.00"),
                    committedObligations = BigDecimal("8100.00"),
                    variableMonthlyBudget = BigDecimal("1300.00"),
                    emergencyReserveBalance = BigDecimal.ZERO,
                ),
            )

        assertMoney("0.00", result.freeRealBalance)
        assertMoney("0.00", result.dailySpendingLimit)
        assertMoney("0.00", result.projectedShortfall)
    }

    @Test
    fun `reports projected shortfall without producing negative daily allowance`() {
        val result =
            FinancialPlanner.calculate(
                baseInput(
                    operatingBalance = BigDecimal("1000.00"),
                    committedObligations = BigDecimal("1200.00"),
                    variableMonthlyBudget = BigDecimal("500.00"),
                ),
            )

        assertMoney("700.00", result.projectedShortfall)
        assertMoney("0.00", result.dailySpendingLimit)
        assertTrue(result.warnings.any { it.contains("não cobre") })
    }

    @Test
    fun `pays high cost debt before reserve and investments`() {
        val result =
            FinancialPlanner.calculate(
                baseInput(
                    operatingBalance = BigDecimal("5000.00"),
                    variableMonthlyBudget = BigDecimal("1000.00"),
                    highCostDebtOutstanding = BigDecimal("2500.00"),
                    emergencyReserveBalance = BigDecimal.ZERO,
                ),
            )

        assertMoney("2500.00", result.debtPaymentRecommendation)
        assertMoney("0.00", result.investmentContribution)
    }

    @Test
    fun `invests only after the emergency reserve is complete`() {
        val result =
            FinancialPlanner.calculate(
                baseInput(
                    operatingBalance = BigDecimal("5000.00"),
                    variableMonthlyBudget = BigDecimal("1000.00"),
                    emergencyReserveBalance = BigDecimal("6000.00"),
                    investmentContributionRate = BigDecimal("0.10"),
                ),
            )

        assertMoney("760.00", result.investmentContribution)
        assertMoney("3240.00", result.freeRealBalance)
    }

    private fun baseInput(
        operatingBalance: BigDecimal,
        committedObligations: BigDecimal = BigDecimal.ZERO,
        variableMonthlyBudget: BigDecimal = BigDecimal.ZERO,
        emergencyReserveBalance: BigDecimal = BigDecimal.ZERO,
        highCostDebtOutstanding: BigDecimal = BigDecimal.ZERO,
        investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    ) = PlannerInput(
        asOf = LocalDate.of(2026, 7, 15),
        payDay = 5,
        monthlyIncome = BigDecimal("7600.00"),
        essentialMonthlyExpenses = BigDecimal("1000.00"),
        variableMonthlyBudget = variableMonthlyBudget,
        variableSpentThisMonth = BigDecimal.ZERO,
        minimumCashBuffer = BigDecimal.ZERO,
        emergencyTargetMonths = 6,
        reserveContributionRate = BigDecimal("0.10"),
        investmentContributionRate = investmentContributionRate,
        operatingBalance = operatingBalance,
        emergencyReserveBalance = emergencyReserveBalance,
        committedObligations = committedObligations,
        highCostDebtOutstanding = highCostDebtOutstanding,
    )

    private fun assertMoney(
        expected: String,
        actual: BigDecimal,
    ) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
