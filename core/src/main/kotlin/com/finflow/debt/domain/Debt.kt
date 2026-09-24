package com.finflow.debt.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Debt(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val debtType: DebtType = DebtType.OTHER,
    val outstandingAmount: BigDecimal = BigDecimal.ZERO,
    val monthlyPayment: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val annualEffectiveRate: BigDecimal = BigDecimal.ZERO,
    val priority: DebtPriority = DebtPriority.REGULAR,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
