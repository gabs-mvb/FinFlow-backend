package com.finflow.obligation.application.model

import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.application.model.MoneyInput
import java.time.LocalDate

data class CreateObligationRequest(
    val name: String,
    val type: ObligationType,
    val amount: MoneyInput,
    val dueDate: LocalDate,
) {
    init {
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..160) { "Tamanho inválido para name" }
    }
}
