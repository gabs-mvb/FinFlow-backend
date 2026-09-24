package com.finflow.transaction.application.port.outbound

import com.finflow.transaction.domain.IdempotencyRecord

interface IdempotencyRecordRepository {
    fun save(value: IdempotencyRecord): IdempotencyRecord

    fun findByUserIdAndOperationAndKeyHash(
        userId: Int,
        operation: String,
        keyHash: String,
    ): IdempotencyRecord?
}
