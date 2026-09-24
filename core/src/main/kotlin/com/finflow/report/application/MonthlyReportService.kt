package com.finflow.report.application

import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.report.application.model.CategoryExpense
import com.finflow.report.application.model.MonthlyFinancialReport
import com.finflow.report.application.port.inbound.MonthlyReportUseCases
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.Money
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Currency

class MonthlyReportService(
    private val currentUser: CurrentUser,
    private val transactionRepository: FinancialTransactionRepository,
    private val profileService: FinancialProfileUseCases,
) : MonthlyReportUseCases {
    private val ignoredExpenseCategories =
        setOf(
            TransactionCategory.TRANSFER,
            TransactionCategory.INVESTMENTS,
        )

    override fun generate(period: YearMonth): MonthlyFinancialReport {
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(profile.currency)
        val from = period.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val to =
            period
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .minusNanos(1)
        val transactions =
            transactionRepository
                .findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(currentUser.id(), from, to)
                .filter { it.currency == profile.currency }
        val income =
            transactions
                .filter {
                    it.transactionType == TransactionType.CREDIT && it.category != TransactionCategory.TRANSFER
                }.sumOf { it.amount }
        val investments =
            transactions
                .filter {
                    it.transactionType == TransactionType.DEBIT && it.category == TransactionCategory.INVESTMENTS
                }.sumOf { it.amount }
        val expenses =
            transactions.filter {
                it.transactionType == TransactionType.DEBIT && it.category !in ignoredExpenseCategories
            }
        val totalExpenses = expenses.sumOf { it.amount }
        val netCashFlow = income - totalExpenses - investments
        val savingsRate =
            if (income > BigDecimal.ZERO) {
                (income - totalExpenses)
                    .multiply(BigDecimal("100"))
                    .divide(income, 2, RoundingMode.HALF_EVEN)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
        val byCategory =
            expenses
                .groupBy { it.category }
                .map { (category, items) ->
                    val amount = items.sumOf { it.amount }
                    val percentage =
                        if (totalExpenses > BigDecimal.ZERO) {
                            amount.multiply(BigDecimal("100")).divide(totalExpenses, 2, RoundingMode.HALF_EVEN)
                        } else {
                            BigDecimal.ZERO.setScale(2)
                        }
                    CategoryExpense(category, Money(amount, currency).toOutput(), percentage)
                }.sortedByDescending { it.amount.amount }
        val priorities =
            buildList {
                if (netCashFlow < BigDecimal.ZERO) add("Eliminar o déficit antes de realizar novos aportes")
                if (investments == BigDecimal.ZERO && netCashFlow > BigDecimal.ZERO) {
                    add("Gerar um plano para direcionar o excedente à reserva ou aos investimentos")
                }
                byCategory.firstOrNull()?.let {
                    add("Revisar ${it.category}: maior categoria de despesa do período")
                }
                if (isEmpty()) add("Manter o orçamento e revisar as metas no próximo fechamento")
            }.take(3)
        return MonthlyFinancialReport(
            period = period.toString(),
            totalIncome = Money(income, currency).toOutput(),
            totalExpenses = Money(totalExpenses, currency).toOutput(),
            investmentContributions = Money(investments, currency).toOutput(),
            netCashFlow = Money(netCashFlow, currency).toOutput(),
            savingsRatePercentage = savingsRate,
            expensesByCategory = byCategory,
            priorities = priorities,
            assumptions =
                listOf(
                    "Transferências internas foram excluídas de receitas e despesas",
                    "Aportes foram separados das despesas de consumo",
                    "O relatório usa somente transações na moeda do perfil",
                ),
        )
    }
}
