package com.finflow.openfinance.application.port.outbound

import com.finflow.openfinance.domain.ConsentStatus
import com.finflow.openfinance.domain.OpenFinanceConsent

interface OpenFinanceConsentRepository {
    fun save(value: OpenFinanceConsent): OpenFinanceConsent

    fun findByUserIdAndProviderIgnoreCaseAndExternalConsentId(
        userId: Int,
        provider: String,
        externalConsentId: String,
    ): OpenFinanceConsent?

    fun findAllByUserId(userId: Int): List<OpenFinanceConsent>

    fun findAllByUserIdAndStatus(
        userId: Int,
        status: ConsentStatus,
    ): List<OpenFinanceConsent>
}
