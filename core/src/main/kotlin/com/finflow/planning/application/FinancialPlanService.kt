package com.finflow.planning.application

import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.AccountPurpose
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.debt.domain.DebtPriority
import com.finflow.debt.domain.DebtStatus
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.ObligationStatus
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PlanRevision
import com.finflow.planning.application.model.UpdateFinancialPlanRequest
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.application.port.outbound.PlanRevisionRepository
import com.finflow.planning.domain.ActionIntent
import com.finflow.planning.domain.ActionIntentStatus
import com.finflow.planning.domain.ActionType
import com.finflow.planning.domain.FinancialPlan
import com.finflow.planning.domain.FinancialPlanner
import com.finflow.planning.domain.PlanActionDraft
import com.finflow.planning.domain.PlanContent
import com.finflow.planning.domain.PlanSource
import com.finflow.planning.domain.PlanSource.RULE_BASED
import com.finflow.planning.domain.PlannedAllocation
import com.finflow.planning.domain.PlannerInput
import com.finflow.planning.domain.PlannerResult
import com.finflow.planning.domain.RiskLevel
import com.finflow.portfolio.application.model.ContributionAllocation
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.portfolio.domain.AssetClass
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.BusinessRuleException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.ResourceNotFoundException
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

class FinancialPlanService(
    private val currentUser: CurrentUser,
    private val profileService: FinancialProfileUseCases,
    private val accountRepository: FinancialAccountRepository,
    private val transactionRepository: FinancialTransactionRepository,
    private val obligationRepository: ObligationRepository,
    private val debtRepository: DebtRepository,
    private val portfolioService: PortfolioUseCases,
    private val consentService: OpenFinanceConsentUseCases,
    private val planRepository: FinancialPlanRepository,
    private val actionRepository: ActionIntentRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
    private val revisionRepository: PlanRevisionRepository,
) : FinancialPlanUseCases {
    private val variableCategories =
        setOf(
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
    override fun generate(asOf: LocalDate): FinancialPlanResponse = calculateAndSave(asOf)

    override fun preview(asOf: LocalDate): FinancialPlanResponse = calculateAndSave(asOf, persist = false)

    override fun update(
        id: UUID,
        request: UpdateFinancialPlanRequest,
    ): FinancialPlanResponse {
        planRepository.lockChangesForUser(currentUser.id())
        val plan =
            planRepository.findByIdAndUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Plano financeiro não encontrado")
        val actions = actionRepository.findAllByPlanIdAndPlanUserId(id, currentUser.id())
        if (plan.details.source != RULE_BASED) {
            throw BusinessRuleException(
                "Edite este plano pelo endpoint /content ou gere uma nova proposta personalizada",
                "PERSONALIZED_PLAN_RECALCULATION",
            )
        }
        if (actions.any { it.requiresApproval && it.status != ActionIntentStatus.PROPOSED }) {
            throw BusinessRuleException("O plano possui ações já revisadas; gere um novo plano", "PLAN_ALREADY_REVIEWED")
        }
        revisionRepository.save(
            PlanRevision(
                plan.id,
                currentUser.id(),
                plan.revision,
                OffsetDateTime.now(clock),
                plan.toResponse(
                    plan.totalBalanceSnapshot ?: plan.operatingBalance,
                    plan.reserveBalanceSnapshot ?: BigDecimal.ZERO,
                    plan.reserveTargetSnapshot ?: BigDecimal.ZERO,
                    plan.allocations.map {
                        ContributionAllocation(
                            it.assetClass,
                            Money(it.amount, Currency.getInstance(plan.currency)).toOutput(),
                            "Alocação do plano",
                        )
                    },
                    actions,
                ),
            ),
        )
        return calculateAndSave(request.asOf, plan, actions)
    }

    private fun calculateAndSave(
        asOf: LocalDate,
        existing: FinancialPlan? = null,
        previousActions: List<ActionIntent> = emptyList(),
        persist: Boolean = true,
    ): FinancialPlanResponse {
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(profile.currency)
        val accountsInCurrency = accountRepository.findAllByUserIdAndCurrency(currentUser.id(), profile.currency)
        val totalBalance = accountsInCurrency.sumOf { it.availableBalance }
        val operatingBalance =
            accountsInCurrency
                .filter { it.purpose == AccountPurpose.OPERATING }
                .sumOf { it.availableBalance }
        val reserveBalance =
            accountsInCurrency
                .filter { it.purpose == AccountPurpose.EMERGENCY_RESERVE }
                .sumOf { it.availableBalance }
        val nextIncomeDate =
            if (asOf.dayOfMonth < profile.payDay) {
                asOf.withDayOfMonth(profile.payDay)
            } else {
                asOf.plusMonths(1).withDayOfMonth(profile.payDay)
            }
        val obligations =
            obligationRepository
                .findAllByUserIdAndStatusAndDueDateBetween(
                    currentUser.id(),
                    ObligationStatus.PENDING,
                    asOf,
                    nextIncomeDate.minusDays(1),
                ).filter { it.currency == profile.currency }
        val committed = obligations.sumOf { it.amount }
        val startOfMonth = asOf.withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val endOfDay =
            asOf
                .plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .minusNanos(1)
        val variableSpent =
            transactionRepository
                .findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(currentUser.id(), startOfMonth, endOfDay)
                .filter { transaction ->
                    transaction.transactionType == TransactionType.DEBIT &&
                        transaction.currency == profile.currency &&
                        transaction.category in variableCategories
                }.sumOf { it.amount }
        val highCostDebt =
            debtRepository
                .findAllByUserIdAndStatus(currentUser.id(), DebtStatus.ACTIVE)
                .filter { it.priority == DebtPriority.HIGH_COST && it.currency == profile.currency }
                .sumOf { it.outstandingAmount }
        val calculated =
            FinancialPlanner.calculate(
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
        val plan =
            run {
                FinancialPlan(
                    id = existing?.id ?: UUID.randomUUID(),
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
                    warnings = warnings.toList(),
                    allocations = allocations.map { PlannedAllocation(it.assetClass, it.amount.amount) },
                    generatedAt = now,
                    revision = existing?.revision?.plus(1) ?: 0,
                    totalBalanceSnapshot = totalBalance,
                    reserveBalanceSnapshot = reserveBalance,
                    reserveTargetSnapshot = calculated.emergencyReserveTarget,
                )
            }.let { if (persist) planRepository.save(it) else it }
        val previousByType = previousActions.associateBy { it.actionType }
        val replacements =
            buildActions(plan, calculated, committed, currency, now).map { action ->
                previousByType[action.actionType]?.let { previous ->
                    action.copy(id = previous.id, createdAt = previous.createdAt)
                } ?: action
            }
        val retainedIds = replacements.map { it.id }.toSet()
        previousActions.filter { it.id !in retainedIds }.forEach { action ->
            actionRepository.deleteByIdAndPlanUserId(action.id, currentUser.id())
        }
        val actions = if (persist) actionRepository.saveAll(replacements) else replacements
        if (persist) {
            auditService.record(
                if (existing ==
                    null
                ) {
                    "FINANCIAL_PLAN_GENERATED"
                } else {
                    "FINANCIAL_PLAN_UPDATED"
                },
                "FINANCIAL_PLAN",
                plan.id,
            )
        }
        return plan.toResponse(
            totalBalance = totalBalance,
            reserveBalance = reserveBalance,
            emergencyTarget = calculated.emergencyReserveTarget,
            allocations = allocations,
            actions = actions,
        )
    }

    /** Retorna o plano mais recente ou `null` quando ainda não houve cálculo. */
    override fun latest(): FinancialPlanResponse? {
        val plan =
            planRepository.findFirstByUserIdOrderByGeneratedAtDesc(currentUser.id())
                ?: return null
        val profile = profileService.getRequired()
        val currency = Currency.getInstance(plan.currency)
        val accounts = accountRepository.findAllByUserIdAndCurrency(currentUser.id(), plan.currency)
        val total = accounts.sumOf { it.availableBalance }
        val reserve =
            accounts
                .filter { it.purpose == AccountPurpose.EMERGENCY_RESERVE }
                .sumOf { it.availableBalance }
        val target = profile.essentialMonthlyExpenses.multiply(profile.emergencyTargetMonths.toBigDecimal())
        return plan.toResponse(
            totalBalance = total,
            reserveBalance = reserve,
            emergencyTarget = target,
            allocations =
                plan.allocations.map {
                    ContributionAllocation(
                        it.assetClass,
                        Money(it.amount, currency).toOutput(),
                        if (plan.details.source == PlanSource.RULE_BASED) {
                            "Classe abaixo da alocação-alvo; aporte direcionado sem vender posições"
                        } else {
                            "Alocação proposta no plano"
                        },
                    )
                },
            actions = actionRepository.findAllByPlanIdAndPlanUserId(plan.id, currentUser.id()),
        )
    }

    /** Registra a decisão do usuário sem executar movimentações financeiras. */
    override fun reviewAction(
        id: UUID,
        approve: Boolean,
    ): ActionIntentResponse {
        planRepository.lockChangesForUser(currentUser.id())
        val action =
            actionRepository.findByIdAndPlanUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Intenção de ação não encontrada")
        if (action.status != ActionIntentStatus.PROPOSED) {
            throw BusinessRuleException("A intenção já foi revisada", "ACTION_ALREADY_REVIEWED")
        }
        val updated =
            action.copy(
                status = if (approve) ActionIntentStatus.APPROVED else ActionIntentStatus.REJECTED,
                reviewedAt = OffsetDateTime.now(clock),
            )
        actionRepository.save(updated)
        auditService.record(
            if (approve) "ACTION_INTENT_APPROVED" else "ACTION_INTENT_REJECTED",
            "ACTION_INTENT",
            action.id,
        )
        return updated.toResponse()
    }

    private fun buildActions(
        plan: FinancialPlan,
        result: PlannerResult,
        committed: BigDecimal,
        currency: Currency,
        now: OffsetDateTime,
    ): List<ActionIntent> =
        buildList {
            if (committed > BigDecimal.ZERO) {
                add(
                    action(
                        plan,
                        ActionType.RESERVE_FOR_OBLIGATIONS,
                        committed,
                        currency,
                        RiskLevel.LOW,
                        requiresApproval = false,
                        rationale = "Valor reservado para obrigações que vencem antes da próxima renda",
                        now = now,
                    ),
                )
            }
            if (result.projectedShortfall > BigDecimal.ZERO) {
                add(
                    action(
                        plan,
                        ActionType.REDUCE_VARIABLE_SPENDING,
                        result.projectedShortfall,
                        currency,
                        RiskLevel.LOW,
                        requiresApproval = false,
                        rationale = "O orçamento precisa cair neste valor para evitar o déficit projetado",
                        now = now,
                    ),
                )
            }
            if (result.debtPaymentRecommendation > BigDecimal.ZERO) {
                add(
                    action(
                        plan,
                        ActionType.PAY_HIGH_COST_DEBT,
                        result.debtPaymentRecommendation,
                        currency,
                        RiskLevel.MEDIUM,
                        requiresApproval = true,
                        rationale = "Pagamento priorizado porque há dívida de alto custo em aberto",
                        now = now,
                    ),
                )
            }
            if (result.reserveContribution > BigDecimal.ZERO) {
                add(
                    action(
                        plan,
                        ActionType.TRANSFER_TO_EMERGENCY_RESERVE,
                        result.reserveContribution,
                        currency,
                        RiskLevel.LOW,
                        requiresApproval = true,
                        rationale = "Valor necessário neste mês para avançar até a meta da reserva",
                        now = now,
                    ),
                )
            }
            if (result.investmentContribution > BigDecimal.ZERO) {
                add(
                    action(
                        plan,
                        ActionType.CREATE_INVESTMENT_CONTRIBUTION,
                        result.investmentContribution,
                        currency,
                        RiskLevel.MEDIUM,
                        requiresApproval = true,
                        rationale = "Aporte disponível após cobrir obrigações, dívida cara e reserva",
                        now = now,
                    ),
                )
            }
        }

    private fun action(
        plan: FinancialPlan,
        type: ActionType,
        amount: BigDecimal,
        currency: Currency,
        riskLevel: RiskLevel,
        requiresApproval: Boolean,
        rationale: String,
        now: OffsetDateTime,
    ) = ActionIntent(
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
}

internal fun FinancialPlan.toResponse(
    totalBalance: BigDecimal,
    reserveBalance: BigDecimal,
    emergencyTarget: BigDecimal,
    allocations: List<ContributionAllocation>,
    actions: List<ActionIntent>,
): FinancialPlanResponse {
    val parsedCurrency = Currency.getInstance(currency)

    fun money(value: BigDecimal) = Money(value, parsedCurrency).toOutput()
    return FinancialPlanResponse(
        id = id,
        asOf = asOf,
        nextIncomeDate = nextIncomeDate,
        totalConsolidatedBalance = money(totalBalanceSnapshot ?: totalBalance),
        operatingBalance = money(operatingBalance),
        emergencyReserveBalance = money(reserveBalanceSnapshot ?: reserveBalance),
        emergencyReserveTarget = money(reserveTargetSnapshot ?: emergencyTarget),
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
        warnings = warnings,
        generatedAt = generatedAt,
        updatedAt = updatedAt,
        revision = revision,
        details = details,
        content =
            PlanContent(
                asOf,
                nextIncomeDate,
                details.summary.ifBlank { "Plano financeiro baseado nas regras do perfil" },
                details.analysis.ifBlank { "Cálculo a partir dos saldos, compromissos e preferências cadastradas." },
                (reserveTargetSnapshot ?: emergencyTarget).setScale(2),
                remainingVariableBudget.setScale(2),
                minimumCashBuffer.setScale(2),
                debtPaymentRecommendation.setScale(2),
                reserveContribution.setScale(2),
                investmentContribution.setScale(2),
                dailySpendingLimit.setScale(2),
                details.categoryBudgets,
                this.allocations.map { it.copy(amount = it.amount.setScale(2)) },
                actions.map { PlanActionDraft(it.actionType, it.amount.setScale(2), it.riskLevel, it.rationale) },
                warnings,
            ),
    )
}

private fun ActionIntent.toResponse(): ActionIntentResponse =
    ActionIntentResponse(
        id = id,
        type = actionType,
        amount = Money(amount, Currency.getInstance(currency)).toOutput(),
        riskLevel = riskLevel,
        requiresApproval = requiresApproval,
        status = status,
        rationale = rationale,
    )
