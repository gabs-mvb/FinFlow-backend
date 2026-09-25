package com.finflow.planning.application

import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.debt.domain.DebtStatus
import com.finflow.goal.application.port.outbound.GoalRepository
import com.finflow.goal.domain.GoalStatus
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.ObligationStatus
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import com.finflow.planning.application.model.MonthlyCashFlow
import com.finflow.planning.application.model.PersonalizationContext
import com.finflow.planning.application.model.PlanningDebt
import com.finflow.planning.application.model.PlanningEvidence
import com.finflow.planning.application.model.PlanningGoal
import com.finflow.planning.application.model.PlanningObligation
import com.finflow.planning.application.model.PlanningPosition
import com.finflow.planning.application.model.PlanningProfile
import com.finflow.planning.application.model.SpendingBucket
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class PlanningContextReader(
    private val user: CurrentUser,
    private val profiles: FinancialProfileUseCases,
    private val plans: FinancialPlanUseCases,
    private val transactions: FinancialTransactionRepository,
    private val debts: DebtRepository,
    private val goals: GoalRepository,
    private val obligations: ObligationRepository,
    private val portfolio: PortfolioUseCases,
    private val consent: OpenFinanceConsentUseCases,
) {
    fun read(asOf: LocalDate): PersonalizationContext {
        val owner = user.id()
        val profile = profiles.getRequired()
        val baseline = plans.preview(asOf)
        val from = asOf.minusDays(89)
        val all =
            transactions.findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                owner,
                from.atStartOfDay().atOffset(ZoneOffset.UTC),
                asOf
                    .plusDays(1)
                    .atStartOfDay()
                    .atOffset(ZoneOffset.UTC)
                    .minusNanos(1),
            )
        val history = all.filter { it.currency == profile.currency && it.category != TransactionCategory.TRANSFER }
        val buckets =
            history
                .filter { it.transactionType == TransactionType.DEBIT }
                .groupBy { YearMonth.from(it.occurredAt.withOffsetSameInstant(ZoneOffset.UTC)).toString() to it.category }
                .map { (key, values) -> SpendingBucket(key.first, key.second, values.sumOf { it.amount }, values.size) }
                .sortedWith(compareBy({ it.month }, { it.category.name }))
        val cashFlow =
            generateSequence(YearMonth.from(from)) { it.plusMonths(1) }
                .takeWhile { it <= YearMonth.from(asOf) }
                .map { month ->
                    val items = history.filter { YearMonth.from(it.occurredAt.withOffsetSameInstant(ZoneOffset.UTC)) == month }
                    MonthlyCashFlow(
                        month.toString(),
                        items.filter { it.transactionType == TransactionType.CREDIT }.sumOf { it.amount },
                        items
                            .filter {
                                it.transactionType == TransactionType.DEBIT && it.category != TransactionCategory.INVESTMENTS
                            }.sumOf { it.amount },
                        items
                            .filter {
                                it.transactionType == TransactionType.DEBIT && it.category == TransactionCategory.INVESTMENTS
                            }.sumOf { it.amount },
                    )
                }.toList()
        return PersonalizationContext(
            owner,
            PlanningEvidence(
                asOf,
                from,
                asOf,
                history.size,
                all.count { it.currency != profile.currency },
                PlanningProfile(
                    profile.currency,
                    profile.monthlyIncome,
                    profile.payDay,
                    profile.essentialMonthlyExpenses,
                    profile.variableMonthlyBudget,
                    profile.minimumCashBuffer,
                    profile.emergencyTargetMonths,
                    profile.reserveContributionRate,
                    profile.investmentContributionRate,
                    profile.riskProfile,
                    profile.autopilotMode,
                ),
                baseline.operatingBalance.amount,
                baseline.totalConsolidatedBalance.amount,
                baseline.emergencyReserveBalance.amount,
                consent.hasActiveConsent(),
                cashFlow,
                buckets,
                debts
                    .findAllByUserIdAndStatus(owner, DebtStatus.ACTIVE)
                    .filter { it.currency == profile.currency }
                    .map { PlanningDebt(it.debtType.name, it.outstandingAmount, it.monthlyPayment, it.annualEffectiveRate) }
                    .sortedWith(compareBy({ it.type }, { it.outstanding }, { it.monthlyPayment }, { it.annualRate })),
                goals
                    .findAllByUserId(owner)
                    .filter { it.currency == profile.currency && it.status == GoalStatus.ACTIVE }
                    .map { PlanningGoal(it.targetAmount, it.currentAmount, it.targetDate, it.priority) }
                    .sortedWith(compareBy({ it.priority }, { it.targetDate }, { it.targetAmount }, { it.currentAmount })),
                obligations
                    .findAllByUserId(owner)
                    .filter { it.currency == profile.currency && it.status == ObligationStatus.PENDING }
                    .flatMap { obligation ->
                        obligation.occurrencesUntil(asOf.plusDays(367)).map { dueDate ->
                            PlanningObligation(obligation.obligationType.name, obligation.amount, dueDate)
                        }
                    }.sortedWith(compareBy({ it.dueDate }, { it.type }, { it.amount })),
                portfolio
                    .get()
                    .positions
                    .filter { it.currentValue.currency == profile.currency }
                    .groupBy { it.assetClass }
                    .map { (assetClass, items) ->
                        PlanningPosition(assetClass.name, items.sumOf { it.currentValue.amount })
                    }.sortedBy { it.assetClass },
                requireNotNull(baseline.content),
            ),
        )
    }
}
