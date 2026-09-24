package com.finflow.goal.application.port.outbound

import com.finflow.goal.domain.Goal
import java.util.UUID

interface GoalRepository {
    fun save(value: Goal): Goal

    fun findAllByUserId(userId: Int): List<Goal>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Goal?
}
