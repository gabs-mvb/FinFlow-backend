package com.finflow.financialevent.adapter.inbound.http

import com.finflow.financialevent.application.model.ImportFinancialEventsResponse
import com.finflow.financialevent.application.port.inbound.FinancialEventUseCases
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/financial-events")
class FinancialEventController(
    private val service: FinancialEventUseCases,
) {
    /**
     * Receives parsed Android notifications only. The authenticated transaction
     * import use case resolves the account owner; this contract has no userId.
     */
    @PostMapping("/batch")
    fun importBatch(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: ImportFinancialEventsRequestBody,
    ): ImportFinancialEventsResponse = service.importBatch(idempotencyKey, request.toCommand())
}
