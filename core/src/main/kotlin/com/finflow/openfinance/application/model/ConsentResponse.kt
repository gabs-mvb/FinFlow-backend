package com.finflow.openfinance.application.model

import com.finflow.openfinance.domain.ConsentScope
import com.finflow.openfinance.domain.ConsentStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ConsentResponse(
    val id: UUID,
    val provider: String,
    val externalConsentId: String,
    val institution: String,
    val scopes: Set<ConsentScope>,
    val status: ConsentStatus,
    val expiresAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime,
)
