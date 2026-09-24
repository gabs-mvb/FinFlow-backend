package com.finflow.audit.domain

import java.time.OffsetDateTime
import java.util.UUID

data class AuditEvent(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val actor: String = "owner",
    val action: String = "",
    val resourceType: String = "",
    val resourceId: String? = null,
    val details: String? = null,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
