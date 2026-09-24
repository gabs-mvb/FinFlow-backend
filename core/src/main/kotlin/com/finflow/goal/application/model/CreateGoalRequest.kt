package com.finflow.goal.application.model

import com.finflow.shared.application.model.MoneyInput
import java.math.BigDecimal
import java.time.LocalDate

data class CreateGoalRequest(
    val name: String,
    val targetAmount: MoneyInput,
    val currentAmount: MoneyInput = MoneyInput(BigDecimal.ZERO),
    val targetDate: LocalDate? = null,
    val priority: Int = 3,
) {
    init {
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..160) { "Tamanho inválido para name" }
        require(priority >= 1) { "Valor inválido para priority" }
        require(priority <= 5) { "Valor inválido para priority" }
    }
}
