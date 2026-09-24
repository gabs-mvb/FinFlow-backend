package com.finflow.openfinance.adapter.outbound.persistence

import com.finflow.openfinance.domain.ConsentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "open_finance_consents")
class OpenFinanceConsentEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
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
