package com.finflow.audit.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "audit_events")
class AuditEventEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80)
    var actor: String = "owner",
    @Column(nullable = false, length = 100)
    var action: String = "",
    @Column(name = "resource_type", nullable = false, length = 80)
    var resourceType: String = "",
    @Column(name = "resource_id", length = 100)
    var resourceId: String? = null,
    @Column(length = 1000)
    var details: String? = null,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
