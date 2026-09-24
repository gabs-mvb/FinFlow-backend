package com.finflow.transaction.application.model

import java.util.UUID

data class ImportTransactionsRequest(
    val accountId: UUID,
    val transactions: List<ImportTransactionItem>,
) {
    init {
        require(transactions.size in 1..1000) { "Tamanho inválido para transactions" }
    }
}
