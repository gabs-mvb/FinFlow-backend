package com.finflow.audit.adapter.outbound.persistence

import com.finflow.audit.domain.AuditEvent

internal fun AuditEventEntity.toDomain(): AuditEvent =
    AuditEvent(
        userId = userId,
        id = id,
        actor = actor,
        action = action,
        resourceType = resourceType,
        resourceId = resourceId,
        details = details,
        occurredAt = occurredAt,
    )

internal fun AuditEvent.toEntity(): AuditEventEntity =
    AuditEventEntity(
        userId = userId,
        id = id,
        actor = actor,
        action = action,
        resourceType = resourceType,
        resourceId = resourceId,
        details = details,
        occurredAt = occurredAt,
    )
