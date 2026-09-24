package com.finflow.goal.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataGoalRepository : JpaRepository<GoalEntity, UUID> {
    fun findAllByUserId(userId: Int): List<GoalEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): GoalEntity?
}
