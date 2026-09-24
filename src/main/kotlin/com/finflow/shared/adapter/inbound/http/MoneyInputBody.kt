package com.finflow.shared.adapter.inbound.http

import com.finflow.shared.application.model.MoneyInput
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class MoneyInputBody(
    @field:DecimalMin("0.00")
    @field:Digits(integer = 17, fraction = 2)
    val amount: BigDecimal,
    @field:Pattern(regexp = "[A-Za-z]{3}")
    val currency: String = "BRL",
)

fun MoneyInputBody.toCommand(): MoneyInput =
    MoneyInput(
        amount = amount,
        currency = currency,
    )
