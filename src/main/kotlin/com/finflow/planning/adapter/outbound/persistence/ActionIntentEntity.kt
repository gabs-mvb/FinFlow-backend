package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.domain.ActionIntentStatus
import com.finflow.planning.domain.ActionType
import com.finflow.planning.domain.RiskLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "action_intents")
class ActionIntentEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: FinancialPlanEntity = FinancialPlanEntity(),
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 48)
    var actionType: ActionType = ActionType.RESERVE_FOR_OBLIGATIONS,
    @Column(nullable = false, precision = 19, scale = 2) var amount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    var riskLevel: RiskLevel = RiskLevel.LOW,
    @Column(name = "requires_approval", nullable = false) var requiresApproval: Boolean = true,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var status: ActionIntentStatus = ActionIntentStatus.PROPOSED,
    @Column(nullable = false, length = 500) var rationale: String = "",
    @Column(name = "created_at", nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "reviewed_at") var reviewedAt: OffsetDateTime? = null,
)
