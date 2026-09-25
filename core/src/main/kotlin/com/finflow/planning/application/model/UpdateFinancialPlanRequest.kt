package com.finflow.planning.application.model

import java.time.LocalDate

data class UpdateFinancialPlanRequest(
    val asOf: LocalDate,
)
