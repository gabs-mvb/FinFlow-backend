package com.finflow.transaction.adapter.outbound.persistence

import com.finflow.transaction.application.port.outbound.IdempotencyRecordRepository
import com.finflow.transaction.domain.IdempotencyRecord
import org.springframework.stereotype.Repository

@Repository
class JpaIdempotencyRecordRepository(
    private val delegate: SpringDataIdempotencyRecordRepository,
) : IdempotencyRecordRepository {
    override fun save(value: IdempotencyRecord): IdempotencyRecord = delegate.save(value.toEntity()).toDomain()

    override fun findByUserIdAndOperationAndKeyHash(
        userId: Int,
        operation: String,
        keyHash: String,
    ): IdempotencyRecord? = delegate.findByUserIdAndOperationAndKeyHash(userId, operation, keyHash)?.toDomain()
}
