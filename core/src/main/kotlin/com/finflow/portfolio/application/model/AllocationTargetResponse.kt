package com.finflow.portfolio.application.model

import com.finflow.portfolio.domain.AssetClass
import java.math.BigDecimal

data class AllocationTargetResponse(
    val assetClass: AssetClass,
    val targetPercentage: BigDecimal,
    val minimumPercentage: BigDecimal,
    val maximumPercentage: BigDecimal,
)
