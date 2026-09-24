package com.finflow.planning.application.port.inbound

import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import java.time.LocalDate
import java.util.UUID

interface FinancialPlanUseCases {
    fun generate(asOf: LocalDate): FinancialPlanResponse

    fun latest(): FinancialPlanResponse?

    fun reviewAction(
        id: UUID,
        approve: Boolean,
    ): ActionIntentResponse
}
