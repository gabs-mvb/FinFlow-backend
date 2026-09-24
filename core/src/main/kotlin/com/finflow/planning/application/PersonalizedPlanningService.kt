package com.finflow.planning.application

import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizedPlanRequest
import com.finflow.planning.application.port.inbound.PersonalizedPlanningUseCases
import com.finflow.planning.application.port.outbound.PersonalizedPlanStore
import com.finflow.planning.application.port.outbound.PlanAdvisor
import com.finflow.shared.domain.AiPlanningException

class PersonalizedPlanningService(
    private val store: PersonalizedPlanStore,
    private val advisor: PlanAdvisor,
) : PersonalizedPlanningUseCases {
    override fun generate(request: PersonalizedPlanRequest): FinancialPlanResponse {
        val context = store.readContext(request.asOf)
        val proposal = advisor.suggest(context.evidence, request.preferences)
        try {
            require(proposal.content.asOf == request.asOf) { "A IA alterou a data de referência" }
            require(proposal.content.nextIncomeDate == context.evidence.baseline.nextIncomeDate) { "A IA alterou a data da renda" }
            val committed =
                context.evidence.obligations
                    .filter {
                        it.dueDate.isBefore(proposal.content.nextIncomeDate)
                    }.sumOf { it.amount }
            proposal.content.validate(context.evidence.operatingBalance, committed)
            require(proposal.content.debtPaymentRecommendation <= context.evidence.debts.sumOf { it.outstanding })
        } catch (_: IllegalArgumentException) {
            throw AiPlanningException("A IA retornou um plano financeiramente inconsistente", "AI_PLAN_INVALID")
        }
        return store.save(context, proposal)
    }
}
