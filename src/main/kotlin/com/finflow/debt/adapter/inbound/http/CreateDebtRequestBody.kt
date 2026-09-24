package com.finflow.debt.adapter.inbound.http

import com.finflow.debt.application.model.CreateDebtRequest
import com.finflow.debt.domain.DebtPriority
import com.finflow.debt.domain.DebtType
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateDebtRequestBody(
    @field:NotBlank @field:Size(max = 160) val name: String,
    val type: DebtType,
    @field:Valid val outstandingAmount: MoneyInputBody,
    @field:Valid val monthlyPayment: MoneyInputBody,
    @field:DecimalMin("0.0") val annualEffectiveRate: BigDecimal,
    val priority: DebtPriority,
)

fun CreateDebtRequestBody.toCommand(): CreateDebtRequest =
    CreateDebtRequest(
        name = name,
        type = type,
        outstandingAmount = outstandingAmount.toCommand(),
        monthlyPayment = monthlyPayment.toCommand(),
        annualEffectiveRate = annualEffectiveRate,
        priority = priority,
    )
