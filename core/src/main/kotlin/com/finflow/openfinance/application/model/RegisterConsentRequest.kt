package com.finflow.openfinance.application.model

import com.finflow.openfinance.domain.ConsentScope
import com.finflow.openfinance.domain.ConsentStatus
import java.time.OffsetDateTime

data class RegisterConsentRequest(
    val provider: String,
    val externalConsentId: String,
    val institution: String,
    val scopes: Set<ConsentScope>,
    val status: ConsentStatus,
    val expiresAt: OffsetDateTime? = null,
) {
    init {
        require(provider.isNotBlank()) { "provider não pode estar vazio" }
        require(provider.length in 0..80) { "Tamanho inválido para provider" }
        require(externalConsentId.isNotBlank()) { "externalConsentId não pode estar vazio" }
        require(externalConsentId.length in 0..180) { "Tamanho inválido para externalConsentId" }
        require(institution.isNotBlank()) { "institution não pode estar vazio" }
        require(institution.length in 0..120) { "Tamanho inválido para institution" }
        require(scopes.size in 1..20) { "Tamanho inválido para scopes" }
    }
}
