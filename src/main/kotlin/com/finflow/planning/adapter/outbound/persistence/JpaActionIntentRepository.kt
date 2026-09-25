package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.domain.ActionIntent
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaActionIntentRepository(
    private val delegate: SpringDataActionIntentRepository,
) : ActionIntentRepository {
    override fun save(value: ActionIntent): ActionIntent = delegate.save(value.toEntity()).toDomain()

    override fun deleteByIdAndPlanUserId(
        id: UUID,
        userId: Int,
    ) = delegate.deleteByIdAndPlanUserId(id, userId)

    override fun saveAll(values: List<ActionIntent>): List<ActionIntent> =
        delegate.saveAll(values.map { it.toEntity() }).map { it.toDomain() }

    override fun findAllByPlanIdAndPlanUserId(
        planId: UUID,
        userId: Int,
    ): List<ActionIntent> =
        delegate.findAllByPlanIdAndPlanUserId(planId, userId).map {
            it.toDomain()
        }

    override fun findByIdAndPlanUserId(
        id: UUID,
        userId: Int,
    ): ActionIntent? = delegate.findByIdAndPlanUserId(id, userId)?.toDomain()
}
