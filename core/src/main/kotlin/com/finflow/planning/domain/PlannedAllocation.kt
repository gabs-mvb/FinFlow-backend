package com.finflow.planning.domain

import com.finflow.portfolio.domain.AssetClass
import java.math.BigDecimal

data class PlannedAllocation(
    val assetClass: AssetClass,
    val amount: BigDecimal,
)
