package com.finflow.portfolio.adapter.inbound.http

import com.finflow.portfolio.application.model.AllocationTargetInput
import com.finflow.portfolio.domain.AssetClass
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

data class AllocationTargetInputBody(
    val assetClass: AssetClass,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val targetPercentage: BigDecimal,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val minimumPercentage: BigDecimal,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val maximumPercentage: BigDecimal,
)

fun AllocationTargetInputBody.toCommand(): AllocationTargetInput =
    AllocationTargetInput(
        assetClass = assetClass,
        targetPercentage = targetPercentage,
        minimumPercentage = minimumPercentage,
        maximumPercentage = maximumPercentage,
    )
