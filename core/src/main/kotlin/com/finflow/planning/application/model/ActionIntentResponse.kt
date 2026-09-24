package com.finflow.planning.application.model

import com.finflow.planning.domain.ActionIntentStatus
import com.finflow.planning.domain.ActionType
import com.finflow.planning.domain.RiskLevel
import com.finflow.shared.application.model.MoneyOutput
import java.util.UUID

data class ActionIntentResponse(
    val id: UUID,
    val type: ActionType,
    val amount: MoneyOutput,
    val riskLevel: RiskLevel,
    val requiresApproval: Boolean,
    val status: ActionIntentStatus,
    val rationale: String,
    val executionAvailable: Boolean = false,
)
