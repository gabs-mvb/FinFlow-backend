package com.finflow.goal.application.port.inbound

import com.finflow.goal.application.model.CreateGoalRequest
import com.finflow.goal.application.model.GoalResponse
import com.finflow.goal.application.model.UpdateGoalProgressRequest
import java.util.UUID

interface GoalUseCases {
    fun create(request: CreateGoalRequest): GoalResponse

    fun list(): List<GoalResponse>

    fun updateProgress(
        id: UUID,
        request: UpdateGoalProgressRequest,
    ): GoalResponse
}
