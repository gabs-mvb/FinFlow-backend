package com.finflow.composition

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.goal.application.port.outbound.GoalRepository
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import com.finflow.planning.adapter.outbound.ai.OpenAiPlanAdvisor
import com.finflow.planning.application.EditablePlanningService
import com.finflow.planning.application.PersonalizedPlanningService
import com.finflow.planning.application.PlanningContextReader
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizationContext
import com.finflow.planning.application.model.PlanProposal
import com.finflow.planning.application.model.PlanRevisionResponse
import com.finflow.planning.application.model.ReplacePlanContentRequest
import com.finflow.planning.application.port.inbound.EditablePlanningUseCases
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.application.port.inbound.PersonalizedPlanningUseCases
import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.application.port.outbound.PersonalizedPlanStore
import com.finflow.planning.application.port.outbound.PlanAdvisor
import com.finflow.planning.application.port.outbound.PlanRevisionRepository
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Configuration
class PersonalizedPlanningConfiguration {
    @Bean
    fun planAdvisor(
        json: ObjectMapper,
        @Value("\${finflow.planning.ai.api-key:}") key: String,
        @Value("\${finflow.planning.ai.model:}") model: String,
        @Value("\${finflow.planning.ai.enabled:false}") enabled: Boolean,
        @Value("\${finflow.planning.ai.timeout-seconds:60}") timeout: Long,
        @Value("\${finflow.planning.ai.responses-uri}") endpoint: URI,
    ): PlanAdvisor {
        require(timeout in 1..180) { "Timeout da IA deve estar entre 1 e 180 segundos" }
        return OpenAiPlanAdvisor(json, key, model, enabled, Duration.ofSeconds(timeout), endpoint)
    }

    @Bean
    fun planningContextReader(
        user: CurrentUser,
        profiles: FinancialProfileUseCases,
        plans: FinancialPlanUseCases,
        transactions: FinancialTransactionRepository,
        debts: DebtRepository,
        goals: GoalRepository,
        obligations: ObligationRepository,
        portfolio: PortfolioUseCases,
        consent: OpenFinanceConsentUseCases,
    ) = PlanningContextReader(user, profiles, plans, transactions, debts, goals, obligations, portfolio, consent)

    @Bean
    fun editablePlanningTarget(
        user: CurrentUser,
        reader: PlanningContextReader,
        plans: FinancialPlanRepository,
        actions: ActionIntentRepository,
        history: PlanRevisionRepository,
        audit: AuditUseCases,
        clock: Clock,
    ) = EditablePlanningService(user, reader, plans, actions, history, audit, clock)

    @Bean
    fun personalizedPlanning(
        target: EditablePlanningService,
        advisor: PlanAdvisor,
        transactions: TransactionTemplate,
    ): PersonalizedPlanningUseCases {
        val store =
            object : PersonalizedPlanStore {
                override fun readContext(asOf: LocalDate): PersonalizationContext =
                    requireNotNull(transactions.execute { target.readContext(asOf) })

                override fun save(
                    context: PersonalizationContext,
                    proposal: PlanProposal,
                ): FinancialPlanResponse = requireNotNull(transactions.execute { target.save(context, proposal) })
            }
        // No transaction spans the network request.
        return PersonalizedPlanningService(store, advisor)
    }

    @Bean
    @Primary
    fun editablePlanning(
        target: EditablePlanningService,
        transactions: TransactionTemplate,
    ): EditablePlanningUseCases =
        object : EditablePlanningUseCases {
            override fun get(id: UUID): FinancialPlanResponse = requireNotNull(transactions.execute { target.get(id) })

            override fun replace(
                id: UUID,
                request: ReplacePlanContentRequest,
            ): FinancialPlanResponse = requireNotNull(transactions.execute { target.replace(id, request) })

            override fun revisions(id: UUID): List<PlanRevisionResponse> = requireNotNull(transactions.execute { target.revisions(id) })
        }
}
