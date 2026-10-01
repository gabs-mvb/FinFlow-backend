package com.finflow.transaction.application.model

import java.util.UUID

data class ImportTransactionsRequest(
    val accountId: UUID,
    val transactions: List<ImportTransactionItem>,
    /**
     * Identifies the logical API operation for idempotency. It is intentionally
     * not exposed by the HTTP transaction-import contract: specialized inbound
     * adapters may use their own namespace without sharing replay records.
     */
    val idempotencyOperation: String = "TRANSACTION_IMPORT",
    /**
     * Notification-derived events are the source of the FinFlow-tracked
     * balance. Other imports preserve their historical non-mutating behavior.
     */
    val adjustTrackedBalance: Boolean = false,
    /** Credit-card purchases affect the expense ledger, not the bank's available cash. Server-only. */
    val balanceExcludedExternalIds: Set<String> = emptySet(),
) {
    init {
        require(transactions.size in 1..1000) { "Tamanho inválido para transactions" }
        require(idempotencyOperation.matches(Regex("[A-Z_]{1,80}"))) { "Operação de idempotência inválida" }
    }
}
