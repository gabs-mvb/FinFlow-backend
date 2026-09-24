package com.finflow.obligation.adapter.inbound.http

import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateObligationRequestBody(
    @field:NotBlank @field:Size(max = 160)
    val name: String,
    val type: ObligationType,
    @field:Valid val amount: MoneyInputBody,
    val dueDate: LocalDate,
)

fun CreateObligationRequestBody.toCommand(): CreateObligationRequest =
    CreateObligationRequest(
        name = name,
        type = type,
        amount = amount.toCommand(),
        dueDate = dueDate,
    )
