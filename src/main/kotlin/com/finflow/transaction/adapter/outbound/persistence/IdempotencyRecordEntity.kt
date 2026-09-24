package com.finflow.transaction.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_operation_key",
            columnNames = ["user_id", "operation", "key_hash"],
        ),
    ],
)
class IdempotencyRecordEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80)
    var operation: String = "",
    @Column(name = "key_hash", nullable = false, length = 64)
    var keyHash: String = "",
    @Column(name = "request_hash", nullable = false, length = 64)
    var requestHash: String = "",
    @Column(name = "imported_count", nullable = false)
    var importedCount: Int = 0,
    @Column(name = "duplicate_count", nullable = false)
    var duplicateCount: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
