package com.finflow.goal.adapter.outbound.persistence

import com.finflow.goal.domain.GoalStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "financial_goals")
class GoalEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 160) var name: String = "",
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    var targetAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
    var currentAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "target_date") var targetDate: LocalDate? = null,
    @Column(nullable = false) var priority: Int = 3,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24) var status: GoalStatus = GoalStatus.ACTIVE,
    @Column(name = "created_at", nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
