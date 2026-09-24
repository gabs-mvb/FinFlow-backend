package com.finflow.transaction.adapter.outbound.persistence

import com.finflow.transaction.domain.IdempotencyRecord

internal fun IdempotencyRecordEntity.toDomain(): IdempotencyRecord =
    IdempotencyRecord(
        userId = userId,
        id = id,
        operation = operation,
        keyHash = keyHash,
        requestHash = requestHash,
        importedCount = importedCount,
        duplicateCount = duplicateCount,
        createdAt = createdAt,
    )

internal fun IdempotencyRecord.toEntity(): IdempotencyRecordEntity =
    IdempotencyRecordEntity(
        userId = userId,
        id = id,
        operation = operation,
        keyHash = keyHash,
        requestHash = requestHash,
        importedCount = importedCount,
        duplicateCount = duplicateCount,
        createdAt = createdAt,
    )
