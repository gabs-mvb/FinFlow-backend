package com.finflow.report.application.model

import com.finflow.shared.application.model.MoneyOutput
import java.math.BigDecimal

data class MonthlyFinancialReport(
    val period: String,
    val totalIncome: MoneyOutput,
    val totalExpenses: MoneyOutput,
    val investmentContributions: MoneyOutput,
    val netCashFlow: MoneyOutput,
    val savingsRatePercentage: BigDecimal,
    val expensesByCategory: List<CategoryExpense>,
    val priorities: List<String>,
    val assumptions: List<String>,
)
