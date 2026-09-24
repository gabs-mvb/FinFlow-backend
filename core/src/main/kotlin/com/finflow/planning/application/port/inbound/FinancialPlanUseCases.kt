package com.finflow.planning.application.port.inbound

import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.UpdateFinancialPlanRequest
import java.time.LocalDate
import java.util.UUID

interface FinancialPlanUseCases {
    fun preview(asOf: LocalDate): FinancialPlanResponse

    fun generate(asOf: LocalDate): FinancialPlanResponse

    fun update(
        id: UUID,
        request: UpdateFinancialPlanRequest,
    ): FinancialPlanResponse

    fun latest(): FinancialPlanResponse?

    fun reviewAction(
        id: UUID,
        approve: Boolean,
    ): ActionIntentResponse
}
