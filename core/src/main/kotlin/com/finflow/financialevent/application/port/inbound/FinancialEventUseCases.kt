package com.finflow.financialevent.application.port.inbound

import com.finflow.financialevent.application.model.ImportFinancialEventsRequest
import com.finflow.financialevent.application.model.ImportFinancialEventsResponse

interface FinancialEventUseCases {
    fun importBatch(
        idempotencyKey: String,
        request: ImportFinancialEventsRequest,
    ): ImportFinancialEventsResponse
}
