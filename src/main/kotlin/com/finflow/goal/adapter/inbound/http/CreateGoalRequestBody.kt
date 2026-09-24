package com.finflow.goal.adapter.inbound.http

import com.finflow.goal.application.model.CreateGoalRequest
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateGoalRequestBody(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:Valid val targetAmount: MoneyInputBody,
    @field:Valid val currentAmount: MoneyInputBody = MoneyInputBody(BigDecimal.ZERO),
    val targetDate: LocalDate? = null,
    @field:Min(1) @field:Max(5) val priority: Int = 3,
)

fun CreateGoalRequestBody.toCommand(): CreateGoalRequest =
    CreateGoalRequest(
        name = name,
        targetAmount = targetAmount.toCommand(),
        currentAmount = currentAmount.toCommand(),
        targetDate = targetDate,
        priority = priority,
    )
