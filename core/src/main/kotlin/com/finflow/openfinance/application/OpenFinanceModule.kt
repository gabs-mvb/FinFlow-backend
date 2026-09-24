package com.finflow.openfinance.application

import com.finflow.openfinance.application.model.ConsentResponse
import com.finflow.openfinance.domain.ConsentScope
import com.finflow.openfinance.domain.OpenFinanceConsent

internal fun OpenFinanceConsent.toResponse(): ConsentResponse =
    ConsentResponse(
        id = id,
        provider = provider,
        externalConsentId = externalConsentId,
        institution = institution,
        scopes = scopes,
        status = status,
        expiresAt = expiresAt,
        updatedAt = updatedAt,
    )
