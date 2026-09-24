package com.finflow.portfolio.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PortfolioPosition(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val assetCode: String = "",
    val assetName: String = "",
    val assetClass: AssetClass = AssetClass.CASH,
    val currentValue: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
