package com.finflow.planning.application.port.inbound

import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PlanRevisionResponse
import com.finflow.planning.application.model.ReplacePlanContentRequest
import java.util.UUID

interface EditablePlanningUseCases {
    fun get(id: UUID): FinancialPlanResponse

    fun replace(
        id: UUID,
        request: ReplacePlanContentRequest,
    ): FinancialPlanResponse

    fun revisions(id: UUID): List<PlanRevisionResponse>
}
