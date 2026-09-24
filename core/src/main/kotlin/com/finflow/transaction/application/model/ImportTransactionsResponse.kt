package com.finflow.transaction.application.model

data class ImportTransactionsResponse(
    val imported: Int,
    val duplicates: Int,
    val idempotentReplay: Boolean,
)
