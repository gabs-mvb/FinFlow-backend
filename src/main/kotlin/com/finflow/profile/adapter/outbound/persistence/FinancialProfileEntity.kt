package com.finflow.profile.adapter.outbound.persistence

import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "financial_profiles")
class FinancialProfileEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 3)
    var currency: String = "BRL",
    @Column(name = "monthly_income", nullable = false, precision = 19, scale = 2)
    var monthlyIncome: BigDecimal = BigDecimal.ZERO,
    @Column(name = "pay_day", nullable = false)
    var payDay: Int = 1,
    @Column(name = "essential_monthly_expenses", nullable = false, precision = 19, scale = 2)
    var essentialMonthlyExpenses: BigDecimal = BigDecimal.ZERO,
    @Column(name = "variable_monthly_budget", nullable = false, precision = 19, scale = 2)
    var variableMonthlyBudget: BigDecimal = BigDecimal.ZERO,
    @Column(name = "minimum_cash_buffer", nullable = false, precision = 19, scale = 2)
    var minimumCashBuffer: BigDecimal = BigDecimal.ZERO,
    @Column(name = "emergency_target_months", nullable = false)
    var emergencyTargetMonths: Int = 6,
    @Column(name = "reserve_contribution_rate", nullable = false, precision = 7, scale = 6)
    var reserveContributionRate: BigDecimal = BigDecimal("0.10"),
    @Column(name = "investment_contribution_rate", nullable = false, precision = 7, scale = 6)
    var investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false, length = 32)
    var riskProfile: RiskProfile = RiskProfile.CONSERVATIVE,
    @Enumerated(EnumType.STRING)
    @Column(name = "autopilot_mode", nullable = false, length = 32)
    var autopilotMode: AutopilotMode = AutopilotMode.OBSERVER,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
