package com.finflow.goal.adapter.inbound.http

import com.finflow.goal.application.model.UpdateGoalProgressRequest
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid

data class UpdateGoalProgressRequestBody(
    @field:Valid val currentAmount: MoneyInputBody,
)

fun UpdateGoalProgressRequestBody.toCommand(): UpdateGoalProgressRequest =
    UpdateGoalProgressRequest(
        currentAmount = currentAmount.toCommand(),
    )
