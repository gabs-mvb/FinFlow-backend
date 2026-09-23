package com.finflow.report

import com.finflow.profile.FinancialProfileService
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import com.finflow.transaction.FinancialTransactionRepository
import com.finflow.transaction.TransactionCategory
import com.finflow.transaction.TransactionType
import jakarta.transaction.Transactional
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Currency

data class CategoryExpense(
    val category: TransactionCategory,
    val amount: MoneyOutput,
    val percentageOfExpenses: BigDecimal,
)

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

@Service
class MonthlyReportService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val transactionRepository: FinancialTransactionRepository,
    private val profileService: FinancialProfileService,
) {
    private val ignoredExpenseCategories = setOf(
        TransactionCategory.TRANSFER,
        TransactionCategory.INVESTMENTS,
    )

    @Transactional
    fun generate(period: YearMonth): MonthlyFinancialReport {
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(profile.currency)
        val from = period.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = period.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1)
        val transactions = transactionRepository.findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(currentUser.id(), from, to)
            .filter { it.currency == profile.currency }
        val income = transactions.filter {
            it.transactionType == TransactionType.CREDIT && it.category != TransactionCategory.TRANSFER
        }.sumOf { it.amount }
        val investments = transactions.filter {
            it.transactionType == TransactionType.DEBIT && it.category == TransactionCategory.INVESTMENTS
        }.sumOf { it.amount }
        val expenses = transactions.filter {
            it.transactionType == TransactionType.DEBIT && it.category !in ignoredExpenseCategories
        }
        val totalExpenses = expenses.sumOf { it.amount }
        val netCashFlow = income - totalExpenses - investments
        val savingsRate = if (income > BigDecimal.ZERO) {
            (income - totalExpenses).multiply(BigDecimal("100"))
                .divide(income, 2, RoundingMode.HALF_EVEN)
        } else {
            BigDecimal.ZERO.setScale(2)
        }
        val byCategory = expenses.groupBy { it.category }
            .map { (category, items) ->
                val amount = items.sumOf { it.amount }
                val percentage = if (totalExpenses > BigDecimal.ZERO) {
                    amount.multiply(BigDecimal("100")).divide(totalExpenses, 2, RoundingMode.HALF_EVEN)
                } else BigDecimal.ZERO.setScale(2)
                CategoryExpense(category, Money(amount, currency).toOutput(), percentage)
            }
            .sortedByDescending { it.amount.amount }
        val priorities = buildList {
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
            assumptions = listOf(
                "Transferências internas foram excluídas de receitas e despesas",
                "Aportes foram separados das despesas de consumo",
                "O relatório usa somente transações na moeda do perfil",
            ),
        )
    }
}

@RestController
@RequestMapping("/api/v1/reports")
@Validated
class MonthlyReportController(private val service: MonthlyReportService) {
    @GetMapping("/monthly")
    fun monthly(
        @RequestParam @Min(2000) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int,
    ): MonthlyFinancialReport = service.generate(YearMonth.of(year, month))
}
