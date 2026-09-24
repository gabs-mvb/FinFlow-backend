package com.finflow.goal.adapter.outbound.persistence

import com.finflow.goal.application.port.outbound.GoalRepository
import com.finflow.goal.domain.Goal
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaGoalRepository(
    private val delegate: SpringDataGoalRepository,
) : GoalRepository {
    override fun save(value: Goal): Goal = delegate.save(value.toEntity()).toDomain()

    override fun findAllByUserId(userId: Int): List<Goal> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Goal? = delegate.findByIdAndUserId(id, userId)?.toDomain()
}
