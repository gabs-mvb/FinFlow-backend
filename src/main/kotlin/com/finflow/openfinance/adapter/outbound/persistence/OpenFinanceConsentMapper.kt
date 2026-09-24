package com.finflow.openfinance.adapter.outbound.persistence

import com.finflow.openfinance.domain.ConsentScope
import com.finflow.openfinance.domain.OpenFinanceConsent

internal fun OpenFinanceConsentEntity.toDomain(): OpenFinanceConsent =
    OpenFinanceConsent(
        userId = userId,
        id = id,
        provider = provider,
        externalConsentId = externalConsentId,
        institution = institution,
        scopes =
            scopes
                .split(',')
                .filter { it.isNotBlank() }
                .map { ConsentScope.valueOf(it) }
                .toSet(),
        status = status,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun OpenFinanceConsent.toEntity(): OpenFinanceConsentEntity =
    OpenFinanceConsentEntity(
        userId = userId,
        id = id,
        provider = provider,
        externalConsentId = externalConsentId,
        institution = institution,
        scopes = scopes.joinToString(",") { it.name },
        status = status,
        expiresAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
