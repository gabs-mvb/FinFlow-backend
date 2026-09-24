package com.finflow.openfinance.application.port.inbound

import com.finflow.openfinance.application.model.ConsentResponse
import com.finflow.openfinance.application.model.OpenFinanceIntegrationStatus
import com.finflow.openfinance.application.model.RegisterConsentRequest

interface OpenFinanceConsentUseCases {
    fun register(request: RegisterConsentRequest): ConsentResponse

    fun list(): List<ConsentResponse>

    fun hasActiveConsent(): Boolean

    fun integrationStatus(): OpenFinanceIntegrationStatus
}
