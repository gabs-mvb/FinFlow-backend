package com.finflow.planning.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizationContext
import com.finflow.planning.application.model.PlanProposal
import com.finflow.planning.application.model.PlanRevision
import com.finflow.planning.application.model.PlanRevisionResponse
import com.finflow.planning.application.model.PlanningEvidence
import com.finflow.planning.application.model.ReplacePlanContentRequest
import com.finflow.planning.application.port.inbound.EditablePlanningUseCases
import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.application.port.outbound.PersonalizedPlanStore
import com.finflow.planning.application.port.outbound.PlanRevisionRepository
import com.finflow.planning.domain.ActionIntent
import com.finflow.planning.domain.ActionIntentStatus
import com.finflow.planning.domain.ActionType
import com.finflow.planning.domain.FinancialPlan
import com.finflow.planning.domain.PlanContent
import com.finflow.planning.domain.PlanDetails
import com.finflow.planning.domain.PlanSource
import com.finflow.planning.domain.RiskLevel
import com.finflow.portfolio.application.model.ContributionAllocation
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ConflictException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.ResourceNotFoundException
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

class EditablePlanningService(
    private val user: CurrentUser,
    private val contextReader: PlanningContextReader,
    private val plans: FinancialPlanRepository,
    private val actions: ActionIntentRepository,
    private val history: PlanRevisionRepository,
    private val audit: AuditUseCases,
    private val clock: Clock,
) : PersonalizedPlanStore,
    EditablePlanningUseCases {
    override fun readContext(asOf: LocalDate) = contextReader.read(asOf)

    override fun save(
        context: PersonalizationContext,
        proposal: PlanProposal,
    ): FinancialPlanResponse {
        require(context.ownerId == user.id()) { "Contexto pertence a outro usuário" }
        plans.lockChangesForUser(user.id())
        val fresh = contextReader.read(context.evidence.asOf)
        if (fresh != context) throw ConflictException("Os dados financeiros mudaram; gere novamente", "PLANNING_DATA_CHANGED")
        return persist(
            null,
            proposal.content,
            fresh.evidence,
            PlanDetails(
                PlanSource.AI,
                proposal.content.summary,
                proposal.content.analysis,
                proposal.content.categoryBudgets,
                proposal.model,
                proposal.promptVersion,
            ),
        )
    }

    override fun get(id: UUID): FinancialPlanResponse = response(required(id))

    override fun replace(
        id: UUID,
        request: ReplacePlanContentRequest,
    ): FinancialPlanResponse {
        plans.lockChangesForUser(user.id())
        val previous = required(id)
        if (previous.revision != request.expectedRevision) {
            throw ConflictException("O plano foi alterado; atualize a revisão antes de salvar", "PLAN_REVISION_CONFLICT")
        }
        val evidence = contextReader.read(request.content.asOf).evidence
        require(evidence.profile.currency == previous.currency) { "A moeda do perfil mudou; gere um novo plano" }
        return persist(
            previous,
            request.content,
            evidence,
            previous.details.copy(
                source = PlanSource.MANUAL,
                summary = request.content.summary,
                analysis = request.content.analysis,
                categoryBudgets = request.content.categoryBudgets,
            ),
        )
    }

    override fun revisions(id: UUID): List<PlanRevisionResponse> {
        required(id)
        return history.findAllByPlanIdAndUserId(id, user.id()).map { PlanRevisionResponse(it.revision, it.capturedAt, it.plan) }
    }

    private fun required(id: UUID) =
        plans.findByIdAndUserId(id, user.id())
            ?: throw ResourceNotFoundException("Plano financeiro não encontrado")

    private fun persist(
        previous: FinancialPlan?,
        content: PlanContent,
        evidence: PlanningEvidence,
        details: PlanDetails,
    ): FinancialPlanResponse {
        // Pending overdue obligations also consume cash; they must not disappear when the reference date advances.
        val committed = evidence.obligations.filter { it.dueDate < content.nextIncomeDate }.sumOf { it.amount }
        content.validate(evidence.operatingBalance, committed)
        require(content.debtPaymentRecommendation <= evidence.debts.sumOf { it.outstanding }) { "Pagamento supera a dívida ativa" }
        val now = OffsetDateTime.now(clock)
        if (previous != null) {
            history.save(PlanRevision(previous.id, user.id(), previous.revision, now, response(previous)))
            actions.findAllByPlanIdAndPlanUserId(previous.id, user.id()).forEach { actions.deleteByIdAndPlanUserId(it.id, user.id()) }
        }
        val plan =
            plans.save(
                FinancialPlan(
                    id = previous?.id ?: UUID.randomUUID(),
                    userId = user.id(),
                    currency = evidence.profile.currency,
                    asOf = content.asOf,
                    nextIncomeDate = content.nextIncomeDate,
                    operatingBalance = evidence.operatingBalance,
                    committedObligations = committed,
                    remainingVariableBudget = content.remainingVariableBudget,
                    minimumCashBuffer = content.minimumCashBuffer,
                    debtPaymentRecommendation = content.debtPaymentRecommendation,
                    reserveContribution = content.reserveContribution,
                    investmentContribution = content.investmentContribution,
                    dailySpendingLimit = content.dailySpendingLimit,
                    freeRealBalance = content.freeBalance(evidence.operatingBalance, committed),
                    projectedShortfall = content.shortfall(evidence.operatingBalance, committed),
                    warnings = content.warnings,
                    allocations = content.allocations,
                    generatedAt = previous?.generatedAt ?: now,
                    updatedAt = now,
                    revision = previous?.revision?.plus(1) ?: 0,
                    details = details,
                    totalBalanceSnapshot = evidence.totalBalance,
                    reserveBalanceSnapshot = evidence.reserveBalance,
                    reserveTargetSnapshot = content.emergencyReserveTarget,
                ),
            )
        actions.saveAll(
            content.actions.map { draft ->
                ActionIntent(
                    plan = plan,
                    actionType = draft.type,
                    amount = draft.amount,
                    currency = plan.currency,
                    riskLevel = draft.riskLevel,
                    requiresApproval = true,
                    status = ActionIntentStatus.PROPOSED,
                    rationale = draft.rationale,
                    createdAt = now,
                )
            },
        )
        audit.record(if (previous == null) "PERSONALIZED_PLAN_GENERATED" else "PLAN_CONTENT_REPLACED", "FINANCIAL_PLAN", plan.id)
        return response(plan)
    }

    private fun response(plan: FinancialPlan): FinancialPlanResponse =
        plan.toResponse(
            plan.totalBalanceSnapshot ?: plan.operatingBalance,
            plan.reserveBalanceSnapshot ?: BigDecimal.ZERO,
            plan.reserveTargetSnapshot ?: BigDecimal.ZERO,
            plan.allocations.map {
                ContributionAllocation(
                    it.assetClass,
                    Money(it.amount, Currency.getInstance(plan.currency)).toOutput(),
                    "Alocação proposta no plano",
                )
            },
            actions.findAllByPlanIdAndPlanUserId(plan.id, user.id()),
        )
}
