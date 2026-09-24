package com.finflow.transaction.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataIdempotencyRecordRepository : JpaRepository<IdempotencyRecordEntity, UUID> {
    fun findByUserIdAndOperationAndKeyHash(
        userId: Int,
        operation: String,
        keyHash: String,
    ): IdempotencyRecordEntity?
}
