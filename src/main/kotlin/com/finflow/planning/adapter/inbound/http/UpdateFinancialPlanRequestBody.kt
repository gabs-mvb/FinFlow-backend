package com.finflow.planning.adapter.inbound.http

import com.finflow.planning.application.model.UpdateFinancialPlanRequest
import java.time.LocalDate

data class UpdateFinancialPlanRequestBody(
    val asOf: LocalDate,
)

fun UpdateFinancialPlanRequestBody.toCommand(): UpdateFinancialPlanRequest = UpdateFinancialPlanRequest(asOf)
