package com.finflow.planning.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataActionIntentRepository : JpaRepository<ActionIntentEntity, UUID> {
    fun deleteByIdAndPlanUserId(
        id: UUID,
        userId: Int,
    )

    fun findAllByPlanIdAndPlanUserId(
        planId: UUID,
        userId: Int,
    ): List<ActionIntentEntity>

    fun findByIdAndPlanUserId(
        id: UUID,
        userId: Int,
    ): ActionIntentEntity?
}
