package com.finflow.obligation.application.model

import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.application.model.MoneyInput
import java.time.LocalDate

data class CreateObligationRequest(
    val name: String,
    val type: ObligationType,
    val amount: MoneyInput,
    val dueDate: LocalDate? = null,
    val recurring: Boolean = false,
    val dueDay: Int? = null,
) {
    init {
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..160) { "Tamanho inválido para name" }
        require(
            if (recurring) dueDay in 1..31 else dueDate != null && dueDay == null,
        ) { "Informe uma data única ou um dia mensal entre 1 e 31" }
    }
}
