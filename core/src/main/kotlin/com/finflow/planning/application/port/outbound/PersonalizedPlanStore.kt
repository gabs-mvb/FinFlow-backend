package com.finflow.planning.application.port.outbound

import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizationContext
import com.finflow.planning.application.model.PlanProposal
import java.time.LocalDate

interface PersonalizedPlanStore {
    fun readContext(asOf: LocalDate): PersonalizationContext

    fun save(
        context: PersonalizationContext,
        proposal: PlanProposal,
    ): FinancialPlanResponse
}
