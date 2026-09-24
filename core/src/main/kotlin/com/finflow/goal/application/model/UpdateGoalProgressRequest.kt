package com.finflow.goal.application.model

import com.finflow.shared.application.model.MoneyInput

data class UpdateGoalProgressRequest(
    val currentAmount: MoneyInput,
)
