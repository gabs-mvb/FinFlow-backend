package com.finflow.debt.application.model

import com.finflow.debt.domain.DebtPriority
import com.finflow.debt.domain.DebtStatus
import com.finflow.debt.domain.DebtType
import com.finflow.shared.application.model.MoneyOutput
import java.math.BigDecimal
import java.util.UUID

data class DebtResponse(
    val id: UUID,
    val name: String,
    val type: DebtType,
    val outstandingAmount: MoneyOutput,
    val monthlyPayment: MoneyOutput,
    val annualEffectiveRate: BigDecimal,
    val priority: DebtPriority,
    val status: DebtStatus,
)
