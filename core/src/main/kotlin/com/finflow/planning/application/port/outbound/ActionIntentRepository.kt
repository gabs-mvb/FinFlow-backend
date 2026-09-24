package com.finflow.planning.application.port.outbound

import com.finflow.planning.domain.ActionIntent
import java.util.UUID

interface ActionIntentRepository {
    fun save(value: ActionIntent): ActionIntent

    fun saveAll(values: List<ActionIntent>): List<ActionIntent>

    fun findAllByPlanIdAndPlanUserId(
        planId: UUID,
        userId: Int,
    ): List<ActionIntent>

    fun findByIdAndPlanUserId(
        id: UUID,
        userId: Int,
    ): ActionIntent?
}
