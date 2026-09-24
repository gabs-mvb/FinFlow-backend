package com.finflow.openfinance.adapter.inbound.http

import com.finflow.openfinance.application.model.ConsentResponse
import com.finflow.openfinance.application.model.OpenFinanceIntegrationStatus
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/open-finance")
class OpenFinanceController(
    private val service: OpenFinanceConsentUseCases,
) {
    @GetMapping("/status")
    fun status(): OpenFinanceIntegrationStatus = service.integrationStatus()

    @PutMapping("/consents")
    fun register(
        @Valid @RequestBody request: RegisterConsentRequestBody,
    ): ConsentResponse = service.register(request.toCommand())

    @GetMapping("/consents")
    fun list(): List<ConsentResponse> = service.list()
}
