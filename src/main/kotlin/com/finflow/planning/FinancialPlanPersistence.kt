package com.finflow.planning

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "financial_plans")
class FinancialPlanEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "as_of", nullable = false) var asOf: LocalDate = LocalDate.now(),
    @Column(name = "next_income_date", nullable = false) var nextIncomeDate: LocalDate = LocalDate.now(),
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "operating_balance", nullable = false, precision = 19, scale = 2)
    var operatingBalance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "committed_obligations", nullable = false, precision = 19, scale = 2)
    var committedObligations: BigDecimal = BigDecimal.ZERO,
    @Column(name = "remaining_variable_budget", nullable = false, precision = 19, scale = 2)
    var remainingVariableBudget: BigDecimal = BigDecimal.ZERO,
    @Column(name = "minimum_cash_buffer", nullable = false, precision = 19, scale = 2)
    var minimumCashBuffer: BigDecimal = BigDecimal.ZERO,
    @Column(name = "debt_payment_recommendation", nullable = false, precision = 19, scale = 2)
    var debtPaymentRecommendation: BigDecimal = BigDecimal.ZERO,
    @Column(name = "reserve_contribution", nullable = false, precision = 19, scale = 2)
    var reserveContribution: BigDecimal = BigDecimal.ZERO,
    @Column(name = "investment_contribution", nullable = false, precision = 19, scale = 2)
    var investmentContribution: BigDecimal = BigDecimal.ZERO,
    @Column(name = "free_real_balance", nullable = false, precision = 19, scale = 2)
    var freeRealBalance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "projected_shortfall", nullable = false, precision = 19, scale = 2)
    var projectedShortfall: BigDecimal = BigDecimal.ZERO,
    @Column(name = "daily_spending_limit", nullable = false, precision = 19, scale = 2)
    var dailySpendingLimit: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 2000) var warnings: String = "",
    @Column(name = "allocation_plan", nullable = false, length = 2000) var allocationPlan: String = "",
    @Column(name = "generated_at", nullable = false) var generatedAt: OffsetDateTime = OffsetDateTime.now(),
)

enum class ActionType {
    RESERVE_FOR_OBLIGATIONS,
    REDUCE_VARIABLE_SPENDING,
    PAY_HIGH_COST_DEBT,
    TRANSFER_TO_EMERGENCY_RESERVE,
    CREATE_INVESTMENT_CONTRIBUTION,
}

enum class RiskLevel { LOW, MEDIUM, HIGH }
enum class ActionIntentStatus { PROPOSED, APPROVED, REJECTED }

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

interface FinancialPlanRepository : JpaRepository<FinancialPlanEntity, UUID> {
    fun findFirstByOrderByGeneratedAtDesc(): FinancialPlanEntity?
}

interface ActionIntentRepository : JpaRepository<ActionIntentEntity, UUID> {
    fun findAllByPlanId(planId: UUID): List<ActionIntentEntity>
}

