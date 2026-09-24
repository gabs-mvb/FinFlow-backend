package com.finflow.portfolio.domain

import java.math.BigDecimal
import java.util.UUID

data class AllocationTarget(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val assetClass: AssetClass = AssetClass.CASH,
    val targetPercentage: BigDecimal = BigDecimal.ZERO,
    val minimumPercentage: BigDecimal = BigDecimal.ZERO,
    val maximumPercentage: BigDecimal = BigDecimal.ZERO,
)
