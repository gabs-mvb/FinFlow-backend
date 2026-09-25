package com.finflow.obligation.application.model

import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.application.model.MoneyInput
import java.time.LocalDate

data class UpdateObligationRequest(
    val name: String,
    val type: ObligationType,
    val amount: MoneyInput,
    val dueDate: LocalDate?,
    val status: ObligationStatus,
    val recurring: Boolean = false,
    val dueDay: Int? = null,
) {
    init {
        require(name.isNotBlank() && name.length <= 160) { "Nome do compromisso inválido" }
        require(amount.toMoney().isPositive()) { "O valor da obrigação deve ser maior que zero" }
        require(
            if (recurring) dueDay in 1..31 else dueDate != null && dueDay == null,
        ) { "Informe uma data única ou um dia mensal entre 1 e 31" }
        require(!recurring || status != ObligationStatus.PAID) { "Para compromisso recorrente, registre o pagamento na ocorrência atual" }
    }
}
