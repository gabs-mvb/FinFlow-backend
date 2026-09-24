package com.finflow.openfinance.adapter.inbound.http

import com.finflow.openfinance.application.model.RegisterConsentRequest
import com.finflow.openfinance.domain.ConsentScope
import com.finflow.openfinance.domain.ConsentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class RegisterConsentRequestBody(
    @field:NotBlank @field:Size(max = 80) val provider: String,
    @field:NotBlank @field:Size(max = 180) val externalConsentId: String,
    @field:NotBlank @field:Size(max = 120) val institution: String,
    @field:Size(min = 1, max = 20) val scopes: Set<ConsentScope>,
    val status: ConsentStatus,
    val expiresAt: OffsetDateTime? = null,
)

fun RegisterConsentRequestBody.toCommand(): RegisterConsentRequest =
    RegisterConsentRequest(
        provider = provider,
        externalConsentId = externalConsentId,
        institution = institution,
        scopes = scopes,
        status = status,
        expiresAt = expiresAt,
    )
