package com.finflow.audit.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.audit.application.port.outbound.AuditEventRepository
import com.finflow.audit.domain.AuditEvent
import com.finflow.shared.application.port.outbound.CurrentUser
import java.time.Clock
import java.time.OffsetDateTime

class AuditService(
    private val currentUser: CurrentUser,
    private val repository: AuditEventRepository,
    private val clock: Clock,
) : AuditUseCases {
    override fun record(
        action: String,
        resourceType: String,
        resourceId: Any?,
        details: String?,
    ) {
        repository.save(
            AuditEvent(
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
