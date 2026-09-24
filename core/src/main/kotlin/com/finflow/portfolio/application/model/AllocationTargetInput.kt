package com.finflow.portfolio.application.model

import com.finflow.portfolio.domain.AssetClass
import java.math.BigDecimal

data class AllocationTargetInput(
    val assetClass: AssetClass,
    val targetPercentage: BigDecimal,
    val minimumPercentage: BigDecimal,
    val maximumPercentage: BigDecimal,
) {
    init {
        require(targetPercentage >= java.math.BigDecimal("0.0")) { "Valor inválido para targetPercentage" }
        require(targetPercentage <= java.math.BigDecimal("100.0")) { "Valor inválido para targetPercentage" }
        require(minimumPercentage >= java.math.BigDecimal("0.0")) { "Valor inválido para minimumPercentage" }
        require(minimumPercentage <= java.math.BigDecimal("100.0")) { "Valor inválido para minimumPercentage" }
        require(maximumPercentage >= java.math.BigDecimal("0.0")) { "Valor inválido para maximumPercentage" }
        require(maximumPercentage <= java.math.BigDecimal("100.0")) { "Valor inválido para maximumPercentage" }
    }
}
