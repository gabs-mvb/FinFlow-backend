package com.finflow.transaction.application.model

import com.finflow.shared.application.model.MoneyInput
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import java.time.OffsetDateTime

data class ImportTransactionItem(
    val externalId: String,
    val type: TransactionType,
    val amount: MoneyInput,
    val description: String,
    val merchant: String? = null,
    val category: TransactionCategory? = null,
    val occurredAt: OffsetDateTime,
) {
    init {
        require(externalId.isNotBlank()) { "externalId não pode estar vazio" }
        require(externalId.length in 0..180) { "Tamanho inválido para externalId" }
        require(description.isNotBlank()) { "description não pode estar vazio" }
        require(description.length in 0..300) { "Tamanho inválido para description" }
        require(merchant == null || merchant.length in 0..180) { "Tamanho inválido para merchant" }
    }
}
