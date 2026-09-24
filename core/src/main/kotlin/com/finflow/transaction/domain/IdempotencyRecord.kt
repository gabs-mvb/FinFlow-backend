package com.finflow.transaction.domain

import java.time.OffsetDateTime
import java.util.UUID

data class IdempotencyRecord(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val operation: String = "",
    val keyHash: String = "",
    val requestHash: String = "",
    val importedCount: Int = 0,
    val duplicateCount: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
