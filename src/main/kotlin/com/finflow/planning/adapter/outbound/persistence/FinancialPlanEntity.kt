package com.finflow.planning.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "financial_plans")
class FinancialPlanEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
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
