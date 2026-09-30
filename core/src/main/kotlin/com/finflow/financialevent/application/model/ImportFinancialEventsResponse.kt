package com.finflow.financialevent.application.model

data class ImportFinancialEventsResponse(
    val imported: Int,
    val duplicates: Int,
    val idempotentReplay: Boolean,
)
