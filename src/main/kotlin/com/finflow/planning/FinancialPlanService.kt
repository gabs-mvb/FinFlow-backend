package com.finflow.planning

import com.finflow.account.AccountPurpose
import com.finflow.account.FinancialAccountRepository
import com.finflow.audit.AuditService
import com.finflow.debt.DebtPriority
import com.finflow.debt.DebtRepository
import com.finflow.debt.DebtStatus
import com.finflow.obligation.ObligationRepository
import com.finflow.obligation.ObligationStatus
import com.finflow.openfinance.OpenFinanceConsentService
import com.finflow.portfolio.ContributionAllocation
import com.finflow.portfolio.PortfolioService
import com.finflow.profile.FinancialProfileService
import com.finflow.shared.api.BusinessRuleException
import com.finflow.shared.api.ResourceNotFoundException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import com.finflow.transaction.FinancialTransactionRepository
import com.finflow.transaction.TransactionCategory
import com.finflow.transaction.TransactionType
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

data class ActionIntentResponse(
    val id: UUID,
    val type: ActionType,
    val amount: MoneyOutput,
    val riskLevel: RiskLevel,
    val requiresApproval: Boolean,
    val status: ActionIntentStatus,
    val rationale: String,
    val executionAvailable: Boolean = false,
)

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
)

@Service
class FinancialPlanService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val profileService: FinancialProfileService,
    private val accountRepository: FinancialAccountRepository,
    private val transactionRepository: FinancialTransactionRepository,
    private val obligationRepository: ObligationRepository,
    private val debtRepository: DebtRepository,
    private val portfolioService: PortfolioService,
    private val consentService: OpenFinanceConsentService,
    private val planRepository: FinancialPlanRepository,
    private val actionRepository: ActionIntentRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    private val variableCategories = setOf(
        TransactionCategory.FOOD,
        TransactionCategory.TRANSPORT,
        TransactionCategory.SUBSCRIPTIONS,
        TransactionCategory.LEISURE,
        TransactionCategory.OTHER,
    )

    /**
     * Consolida a posição financeira na data informada, persiste o plano e cria
     * as intenções que poderão ser revisadas pelo usuário.
     */
    @Transactional
    fun generate(asOf: LocalDate): FinancialPlanResponse {
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(profile.currency)
        val accountsInCurrency = accountRepository.findAllByUserIdAndCurrency(currentUser.id(), profile.currency)
        val totalBalance = accountsInCurrency.sumOf { it.availableBalance }
        val operatingBalance = accountsInCurrency.filter { it.purpose == AccountPurpose.OPERATING }
            .sumOf { it.availableBalance }
        val reserveBalance = accountsInCurrency.filter { it.purpose == AccountPurpose.EMERGENCY_RESERVE }
            .sumOf { it.availableBalance }

        val nextIncomeDate = if (asOf.dayOfMonth < profile.payDay) {
            asOf.withDayOfMonth(profile.payDay)
        } else {
            asOf.plusMonths(1).withDayOfMonth(profile.payDay)
        }
        val obligations = obligationRepository.findAllByUserIdAndStatusAndDueDateBetween(
            currentUser.id(),
            ObligationStatus.PENDING,
            asOf,
            nextIncomeDate.minusDays(1),
        ).filter { it.currency == profile.currency }
        val committed = obligations.sumOf { it.amount }

        val startOfMonth = asOf.withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val endOfDay = asOf.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1)
        val variableSpent = transactionRepository
            .findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(currentUser.id(), startOfMonth, endOfDay)
            .filter { transaction ->
                transaction.transactionType == TransactionType.DEBIT &&
                    transaction.currency == profile.currency &&
                    transaction.category in variableCategories
            }
            .sumOf { it.amount }
        val highCostDebt = debtRepository.findAllByUserIdAndStatus(currentUser.id(), DebtStatus.ACTIVE)
            .filter { it.priority == DebtPriority.HIGH_COST && it.currency == profile.currency }
            .sumOf { it.outstandingAmount }

        val calculated = FinancialPlanner.calculate(
            PlannerInput(
                asOf = asOf,
                payDay = profile.payDay,
                monthlyIncome = profile.monthlyIncome,
                essentialMonthlyExpenses = profile.essentialMonthlyExpenses,
                variableMonthlyBudget = profile.variableMonthlyBudget,
                variableSpentThisMonth = variableSpent,
                minimumCashBuffer = profile.minimumCashBuffer,
                emergencyTargetMonths = profile.emergencyTargetMonths,
                reserveContributionRate = profile.reserveContributionRate,
                investmentContributionRate = profile.investmentContributionRate,
                operatingBalance = operatingBalance,
                emergencyReserveBalance = reserveBalance,
                committedObligations = committed,
                highCostDebtOutstanding = highCostDebt,
            ),
        )
        val investmentMoney = Money(calculated.investmentContribution, currency)
        val allocations = portfolioService.allocateContribution(investmentMoney)
        val warnings = calculated.warnings.toMutableList()
        if (!consentService.hasActiveConsent()) {
            warnings += "Não há consentimento Open Finance ativo; os dados podem estar desatualizados"
        }
        val ignoredAccounts = accountRepository.findAllByUserId(currentUser.id()).count { it.currency != profile.currency }
        if (ignoredAccounts > 0) {
            val accountLabel = if (ignoredAccounts == 1) "conta foi excluída" else "contas foram excluídas"
            warnings += "$ignoredAccounts $accountLabel do cálculo porque usa outra moeda"
        }
        if (calculated.investmentContribution > BigDecimal.ZERO && allocations.isEmpty()) {
            warnings += "Aporte calculado sem alocação: configure as metas da carteira"
        }
        val now = OffsetDateTime.now(clock)
        val plan = planRepository.save(
            FinancialPlanEntity(
                userId = currentUser.id(),
                asOf = asOf,
                nextIncomeDate = calculated.nextIncomeDate,
                currency = profile.currency,
                operatingBalance = operatingBalance,
                committedObligations = committed,
                remainingVariableBudget = calculated.remainingVariableBudget,
                minimumCashBuffer = profile.minimumCashBuffer,
                debtPaymentRecommendation = calculated.debtPaymentRecommendation,
                reserveContribution = calculated.reserveContribution,
                investmentContribution = calculated.investmentContribution,
                freeRealBalance = calculated.freeRealBalance,
                projectedShortfall = calculated.projectedShortfall,
                dailySpendingLimit = calculated.dailySpendingLimit,
                warnings = warnings.joinToString("|") { it.replace("|", "/") },
                allocationPlan = encodeAllocations(allocations),
                generatedAt = now,
            ),
        )
        val actions = actionRepository.saveAll(buildActions(plan, calculated, committed, currency, now))
        auditService.record("FINANCIAL_PLAN_GENERATED", "FINANCIAL_PLAN", plan.id)
        return plan.toResponse(
            totalBalance = totalBalance,
            reserveBalance = reserveBalance,
            emergencyTarget = calculated.emergencyReserveTarget,
            allocations = allocations,
            actions = actions,
        )
    }

    /** Retorna o plano mais recente ou `null` quando ainda não houve cálculo. */
    @Transactional
    fun latest(): FinancialPlanResponse? {
        val plan = planRepository.findFirstByUserIdOrderByGeneratedAtDesc(currentUser.id())
            ?: return null
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(plan.currency)
        val accounts = accountRepository.findAllByUserIdAndCurrency(currentUser.id(), plan.currency)
        val total = accounts.sumOf { it.availableBalance }
        val reserve = accounts.filter { it.purpose == AccountPurpose.EMERGENCY_RESERVE }
            .sumOf { it.availableBalance }
        val target = profile.essentialMonthlyExpenses.multiply(profile.emergencyTargetMonths.toBigDecimal())
        return plan.toResponse(
            totalBalance = total,
            reserveBalance = reserve,
            emergencyTarget = target,
            allocations = decodeAllocations(plan.allocationPlan, currency),
            actions = actionRepository.findAllByPlanIdAndPlanUserId(plan.id, currentUser.id()),
        )
    }

    /** Registra a decisão do usuário sem executar movimentações financeiras. */
    @Transactional
    fun reviewAction(id: UUID, approve: Boolean): ActionIntentResponse {
        val action = actionRepository.findByIdAndPlanUserId(id, currentUser.id())
            ?: throw ResourceNotFoundException("Intenção de ação não encontrada")
        if (action.status != ActionIntentStatus.PROPOSED) {
            throw BusinessRuleException("A intenção já foi revisada", "ACTION_ALREADY_REVIEWED")
        }
        action.status = if (approve) ActionIntentStatus.APPROVED else ActionIntentStatus.REJECTED
        action.reviewedAt = OffsetDateTime.now(clock)
        actionRepository.save(action)
        auditService.record(
            if (approve) "ACTION_INTENT_APPROVED" else "ACTION_INTENT_REJECTED",
            "ACTION_INTENT",
            action.id,
        )
        return action.toResponse()
    }

    private fun buildActions(
        plan: FinancialPlanEntity,
        result: PlannerResult,
        committed: BigDecimal,
        currency: Currency,
        now: OffsetDateTime,
    ): List<ActionIntentEntity> = buildList {
        if (committed > BigDecimal.ZERO) add(action(
            plan, ActionType.RESERVE_FOR_OBLIGATIONS, committed, currency, RiskLevel.LOW,
            requiresApproval = false,
            rationale = "Valor reservado para obrigações que vencem antes da próxima renda",
            now = now,
        ))
        if (result.projectedShortfall > BigDecimal.ZERO) add(action(
            plan, ActionType.REDUCE_VARIABLE_SPENDING, result.projectedShortfall, currency, RiskLevel.LOW,
            requiresApproval = false,
            rationale = "O orçamento precisa cair neste valor para evitar o déficit projetado",
            now = now,
        ))
        if (result.debtPaymentRecommendation > BigDecimal.ZERO) add(action(
            plan, ActionType.PAY_HIGH_COST_DEBT, result.debtPaymentRecommendation, currency, RiskLevel.MEDIUM,
            requiresApproval = true,
            rationale = "Pagamento priorizado porque há dívida de alto custo em aberto",
            now = now,
        ))
        if (result.reserveContribution > BigDecimal.ZERO) add(action(
            plan, ActionType.TRANSFER_TO_EMERGENCY_RESERVE, result.reserveContribution, currency, RiskLevel.LOW,
            requiresApproval = true,
            rationale = "Valor necessário neste mês para avançar até a meta da reserva",
            now = now,
        ))
        if (result.investmentContribution > BigDecimal.ZERO) add(action(
            plan, ActionType.CREATE_INVESTMENT_CONTRIBUTION, result.investmentContribution, currency,
            RiskLevel.MEDIUM, requiresApproval = true,
            rationale = "Aporte disponível após cobrir obrigações, dívida cara e reserva",
            now = now,
        ))
    }

    private fun action(
        plan: FinancialPlanEntity,
        type: ActionType,
        amount: BigDecimal,
        currency: Currency,
        riskLevel: RiskLevel,
        requiresApproval: Boolean,
        rationale: String,
        now: OffsetDateTime,
    ) = ActionIntentEntity(
        plan = plan,
        actionType = type,
        amount = amount,
        currency = currency.currencyCode,
        riskLevel = riskLevel,
        requiresApproval = requiresApproval,
        status = if (requiresApproval) ActionIntentStatus.PROPOSED else ActionIntentStatus.APPROVED,
        rationale = rationale,
        createdAt = now,
        reviewedAt = if (requiresApproval) null else now,
    )

    private fun encodeAllocations(allocations: List<ContributionAllocation>): String = allocations.joinToString("|") {
        "${it.assetClass.name}:${it.amount.amount}"
    }

    private fun decodeAllocations(value: String, currency: Currency): List<ContributionAllocation> =
        value.split('|').filter { it.isNotBlank() }.mapNotNull { item ->
            val parts = item.split(':', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            runCatching {
                ContributionAllocation(
                    assetClass = com.finflow.portfolio.AssetClass.valueOf(parts[0]),
                    amount = Money(BigDecimal(parts[1]), currency).toOutput(),
                    reason = "Classe abaixo da alocação-alvo; aporte direcionado sem vender posições",
                )
            }.getOrNull()
        }
}

private fun FinancialPlanEntity.toResponse(
    totalBalance: BigDecimal,
    reserveBalance: BigDecimal,
    emergencyTarget: BigDecimal,
    allocations: List<ContributionAllocation>,
    actions: List<ActionIntentEntity>,
): FinancialPlanResponse {
    val parsedCurrency = Currency.getInstance(currency)
    fun money(value: BigDecimal) = Money(value, parsedCurrency).toOutput()
    return FinancialPlanResponse(
        id = id,
        asOf = asOf,
        nextIncomeDate = nextIncomeDate,
        totalConsolidatedBalance = money(totalBalance),
        operatingBalance = money(operatingBalance),
        emergencyReserveBalance = money(reserveBalance),
        emergencyReserveTarget = money(emergencyTarget),
        committedObligations = money(committedObligations),
        remainingVariableBudget = money(remainingVariableBudget),
        minimumCashBuffer = money(minimumCashBuffer),
        debtPaymentRecommendation = money(debtPaymentRecommendation),
        reserveContribution = money(reserveContribution),
        investmentContribution = money(investmentContribution),
        freeRealBalance = money(freeRealBalance),
        projectedShortfall = money(projectedShortfall),
        dailySpendingLimit = money(dailySpendingLimit),
        contributionAllocation = allocations,
        actions = actions.map { it.toResponse() },
        warnings = warnings.split('|').filter { it.isNotBlank() },
        generatedAt = generatedAt,
    )
}

private fun ActionIntentEntity.toResponse(): ActionIntentResponse = ActionIntentResponse(
    id = id,
    type = actionType,
    amount = Money(amount, Currency.getInstance(currency)).toOutput(),
    riskLevel = riskLevel,
    requiresApproval = requiresApproval,
    status = status,
    rationale = rationale,
)
