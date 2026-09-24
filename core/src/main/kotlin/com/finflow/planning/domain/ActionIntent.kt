package com.finflow.planning.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ActionIntent(
    val id: UUID = UUID.randomUUID(),
    val plan: FinancialPlan = FinancialPlan(),
    val actionType: ActionType = ActionType.RESERVE_FOR_OBLIGATIONS,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresApproval: Boolean = true,
    val status: ActionIntentStatus = ActionIntentStatus.PROPOSED,
    val rationale: String = "",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val reviewedAt: OffsetDateTime? = null,
)
