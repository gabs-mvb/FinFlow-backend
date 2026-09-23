package com.finflow.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import java.time.Clock
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

interface AuditEventRepository : JpaRepository<AuditEventEntity, UUID>

@Service
class AuditService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val repository: AuditEventRepository,
    private val clock: Clock,
) {
    fun record(
        action: String,
        resourceType: String,
        resourceId: Any? = null,
        details: String? = null,
    ) {
        repository.save(
            AuditEventEntity(
                userId = currentUser.id(),
                actor = "user:${currentUser.id()}",
                action = action,
                resourceType = resourceType,
                resourceId = resourceId?.toString(),
                details = details?.take(1000),
                occurredAt = OffsetDateTime.now(clock),
            ),
        )
    }
}
