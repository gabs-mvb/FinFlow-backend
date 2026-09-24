package com.finflow.openfinance.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.openfinance.application.model.ConsentResponse
import com.finflow.openfinance.application.model.OpenFinanceIntegrationStatus
import com.finflow.openfinance.application.model.RegisterConsentRequest
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import com.finflow.openfinance.application.port.outbound.OpenFinanceConsentRepository
import com.finflow.openfinance.domain.ConsentStatus
import com.finflow.openfinance.domain.OpenFinanceConsent
import com.finflow.shared.application.port.outbound.CurrentUser
import java.time.Clock
import java.time.OffsetDateTime

class OpenFinanceConsentService(
    private val currentUser: CurrentUser,
    private val repository: OpenFinanceConsentRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
    private val configuredProvider: String,
) : OpenFinanceConsentUseCases {
    override fun register(request: RegisterConsentRequest): ConsentResponse {
        require(request.status != ConsentStatus.ACTIVE || request.expiresAt?.isAfter(OffsetDateTime.now(clock)) == true) {
            "Consentimento ativo exige uma expiração futura"
        }
        val now = OffsetDateTime.now(clock)
        val entity =
            repository.findByUserIdAndProviderIgnoreCaseAndExternalConsentId(
                currentUser.id(),
                request.provider.trim(),
                request.externalConsentId.trim(),
            ) ?: OpenFinanceConsent(userId = currentUser.id(), createdAt = now)
        val updated =
            entity.copy(
                provider = request.provider.trim(),
                externalConsentId = request.externalConsentId.trim(),
                institution = request.institution.trim(),
                scopes = request.scopes.toSet(),
                status = request.status,
                expiresAt = request.expiresAt,
                updatedAt = now,
            )
        val saved = repository.save(updated)
        auditService.record(
            "OPEN_FINANCE_CONSENT_REGISTERED",
            "OPEN_FINANCE_CONSENT",
            saved.id,
            "provider=${saved.provider};status=${saved.status}",
        )
        return saved.toResponse()
    }

    override fun list(): List<ConsentResponse> = repository.findAllByUserId(currentUser.id()).map { it.toResponse() }

    /** Consulta o relógio da aplicação para desconsiderar consentimentos vencidos. */
    override fun hasActiveConsent(): Boolean = hasActiveConsentAt(OffsetDateTime.now(clock))

    internal fun hasActiveConsentAt(now: OffsetDateTime): Boolean =
        repository
            .findAllByUserIdAndStatus(currentUser.id(), ConsentStatus.ACTIVE)
            .any { consent -> consent.expiresAt?.isAfter(now) == true }

    override fun integrationStatus(): OpenFinanceIntegrationStatus {
        val enabled = configuredProvider.lowercase() != "disabled"
        return OpenFinanceIntegrationStatus(
            configuredProvider = configuredProvider,
            liveSynchronizationAvailable = enabled,
            mode = if (enabled) "PARTNER_ADAPTER" else "CANONICAL_IMPORT_ONLY",
            warning =
                if (enabled) {
                    null
                } else {
                    "Nenhum parceiro regulado foi configurado. Use os endpoints canônicos de importação; não há acesso bancário direto."
                },
        )
    }
}
