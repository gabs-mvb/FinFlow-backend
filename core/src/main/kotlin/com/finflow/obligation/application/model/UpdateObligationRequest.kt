package com.finflow.obligation.application.model

import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.application.model.MoneyInput
import java.time.LocalDate

data class UpdateObligationRequest(
    val name: String,
    val type: ObligationType,
    val amount: MoneyInput,
    val dueDate: LocalDate,
    val status: ObligationStatus,
) {
    init {
        require(name.isNotBlank() && name.length <= 160) { "Nome do compromisso inválido" }
        require(amount.toMoney().isPositive()) { "O valor da obrigação deve ser maior que zero" }
    }
}
