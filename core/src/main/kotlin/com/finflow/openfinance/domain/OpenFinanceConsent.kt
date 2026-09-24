package com.finflow.openfinance.domain

import java.time.OffsetDateTime
import java.util.UUID

data class OpenFinanceConsent(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val provider: String = "",
    val externalConsentId: String = "",
    val institution: String = "",
    val scopes: Set<ConsentScope> = emptySet(),
    val status: ConsentStatus = ConsentStatus.AWAITING_AUTHORIZATION,
    val expiresAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
