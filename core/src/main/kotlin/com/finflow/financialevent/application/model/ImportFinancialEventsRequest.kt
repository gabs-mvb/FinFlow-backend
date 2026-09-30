package com.finflow.financialevent.application.model

import java.util.UUID

data class ImportFinancialEventsRequest(
    val accountId: UUID,
    val events: List<FinancialEventItem>,
) {
    init {
        require(events.size in 1..1000) { "Tamanho inválido para events" }
    }
}
