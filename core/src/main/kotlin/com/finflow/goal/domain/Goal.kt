package com.finflow.goal.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Goal(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val targetAmount: BigDecimal = BigDecimal.ZERO,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val targetDate: LocalDate? = null,
    val priority: Int = 3,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
