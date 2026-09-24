package com.finflow.shared.application.model

import java.math.BigDecimal

data class MoneyOutput(
    val amount: BigDecimal,
    val currency: String,
)
