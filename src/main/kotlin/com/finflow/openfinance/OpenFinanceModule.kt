package com.finflow.openfinance

import jakarta.validation.Valid
import com.finflow.audit.AuditService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

enum class ConsentStatus {
    AWAITING_AUTHORIZATION,
    ACTIVE,
    EXPIRED,
    REVOKED,
    REJECTED,
    FAILED,
}

enum class ConsentScope {
    ACCOUNTS,
    BALANCES,
    TRANSACTIONS,
    CREDIT_CARDS,
    CREDIT_OPERATIONS,
    INVESTMENTS,
}

@Entity
@Table(name = "open_finance_consents")
class OpenFinanceConsentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80) var provider: String = "",
    @Column(name = "external_consent_id", nullable = false, length = 180)
    var externalConsentId: String = "",
    @Column(nullable = false, length = 120) var institution: String = "",
    @Column(nullable = false, length = 1000) var scopes: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40) var status: ConsentStatus = ConsentStatus.AWAITING_AUTHORIZATION,
    @Column(name = "expires_at") var expiresAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface OpenFinanceConsentRepository : JpaRepository<OpenFinanceConsentEntity, UUID> {
    fun findByProviderIgnoreCaseAndExternalConsentId(
        provider: String,
        externalConsentId: String,
    ): OpenFinanceConsentEntity?

    fun findAllByStatus(status: ConsentStatus): List<OpenFinanceConsentEntity>
}

data class RegisterConsentRequest(
    @field:NotBlank @field:Size(max = 80) val provider: String,
    @field:NotBlank @field:Size(max = 180) val externalConsentId: String,
    @field:NotBlank @field:Size(max = 120) val institution: String,
    @field:Size(min = 1, max = 20) val scopes: Set<ConsentScope>,
    val status: ConsentStatus,
    val expiresAt: OffsetDateTime? = null,
)

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

data class OpenFinanceIntegrationStatus(
    val configuredProvider: String,
    val liveSynchronizationAvailable: Boolean,
    val mode: String,
    val warning: String?,
)

@Service
class OpenFinanceConsentService(
    private val repository: OpenFinanceConsentRepository,
    private val auditService: AuditService,
    private val clock: Clock,
    @Value("\${finflow.open-finance.provider}") private val configuredProvider: String,
) {
    @Transactional
    fun register(request: RegisterConsentRequest): ConsentResponse {
        require(request.status != ConsentStatus.ACTIVE || request.expiresAt?.isAfter(OffsetDateTime.now(clock)) == true) {
            "Consentimento ativo exige uma expiração futura"
        }
        val now = OffsetDateTime.now(clock)
        val entity = repository.findByProviderIgnoreCaseAndExternalConsentId(
            request.provider,
            request.externalConsentId,
        ) ?: OpenFinanceConsentEntity(createdAt = now)
        entity.apply {
            provider = request.provider.trim()
            externalConsentId = request.externalConsentId.trim()
            institution = request.institution.trim()
            scopes = request.scopes.joinToString(",") { it.name }
            status = request.status
            expiresAt = request.expiresAt
            updatedAt = now
        }
        val saved = repository.save(entity)
        auditService.record(
            "OPEN_FINANCE_CONSENT_REGISTERED",
            "OPEN_FINANCE_CONSENT",
            saved.id,
            "provider=${saved.provider};status=${saved.status}",
        )
        return saved.toResponse()
    }

    @Transactional
    fun list(): List<ConsentResponse> = repository.findAll().map { it.toResponse() }

    /** Consulta o relógio da aplicação para desconsiderar consentimentos vencidos. */
    @Transactional
    fun hasActiveConsent(): Boolean = hasActiveConsentAt(OffsetDateTime.now(clock))

    internal fun hasActiveConsentAt(now: OffsetDateTime): Boolean =
        repository.findAllByStatus(ConsentStatus.ACTIVE)
            .any { consent -> consent.expiresAt?.isAfter(now) == true }

    fun integrationStatus(): OpenFinanceIntegrationStatus {
        val enabled = configuredProvider.lowercase() != "disabled"
        return OpenFinanceIntegrationStatus(
            configuredProvider = configuredProvider,
            liveSynchronizationAvailable = enabled,
            mode = if (enabled) "PARTNER_ADAPTER" else "CANONICAL_IMPORT_ONLY",
            warning = if (enabled) null else {
                "Nenhum parceiro regulado foi configurado. Use os endpoints canônicos de importação; não há acesso bancário direto."
            },
        )
    }
}

@RestController
@RequestMapping("/api/v1/open-finance")
class OpenFinanceController(private val service: OpenFinanceConsentService) {
    @GetMapping("/status")
    fun status(): OpenFinanceIntegrationStatus = service.integrationStatus()

    @PutMapping("/consents")
    fun register(@Valid @RequestBody request: RegisterConsentRequest): ConsentResponse = service.register(request)

    @GetMapping("/consents")
    fun list(): List<ConsentResponse> = service.list()
}

private fun OpenFinanceConsentEntity.toResponse(): ConsentResponse = ConsentResponse(
    id = id,
    provider = provider,
    externalConsentId = externalConsentId,
    institution = institution,
    scopes = scopes.split(',').filter { it.isNotBlank() }.map { ConsentScope.valueOf(it) }.toSet(),
    status = status,
    expiresAt = expiresAt,
    updatedAt = updatedAt,
)
