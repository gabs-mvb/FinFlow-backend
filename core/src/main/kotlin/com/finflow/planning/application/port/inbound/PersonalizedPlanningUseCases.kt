package com.finflow.planning.application.port.inbound

import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizedPlanRequest

fun interface PersonalizedPlanningUseCases {
    fun generate(request: PersonalizedPlanRequest): FinancialPlanResponse
}
