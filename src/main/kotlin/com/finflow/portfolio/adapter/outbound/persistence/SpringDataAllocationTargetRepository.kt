package com.finflow.portfolio.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataAllocationTargetRepository : JpaRepository<AllocationTargetEntity, UUID> {
    fun findAllByUserId(userId: Int): List<AllocationTargetEntity>

    fun deleteAllByUserId(userId: Int)
}
