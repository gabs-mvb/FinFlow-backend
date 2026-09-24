package com.finflow.obligation.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Obligation(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val obligationType: ObligationType = ObligationType.OTHER,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val dueDate: LocalDate = LocalDate.now(),
    val status: ObligationStatus = ObligationStatus.PENDING,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
