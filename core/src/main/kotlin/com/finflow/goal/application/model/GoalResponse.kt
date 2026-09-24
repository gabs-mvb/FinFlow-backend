package com.finflow.goal.application.model

import com.finflow.goal.domain.GoalStatus
import com.finflow.shared.application.model.MoneyOutput
import java.time.LocalDate
import java.util.UUID

data class GoalResponse(
    val id: UUID,
    val name: String,
    val targetAmount: MoneyOutput,
    val currentAmount: MoneyOutput,
    val targetDate: LocalDate?,
    val priority: Int,
    val status: GoalStatus,
)
