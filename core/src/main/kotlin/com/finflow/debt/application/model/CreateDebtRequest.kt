package com.finflow.debt.application.model

import com.finflow.debt.domain.DebtPriority
import com.finflow.debt.domain.DebtType
import com.finflow.shared.application.model.MoneyInput
import java.math.BigDecimal

data class CreateDebtRequest(
    val name: String,
    val type: DebtType,
    val outstandingAmount: MoneyInput,
    val monthlyPayment: MoneyInput,
    val annualEffectiveRate: BigDecimal,
    val priority: DebtPriority,
) {
    init {
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..160) { "Tamanho inválido para name" }
        require(annualEffectiveRate >= java.math.BigDecimal("0.0")) { "Valor inválido para annualEffectiveRate" }
    }
}
