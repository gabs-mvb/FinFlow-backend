package com.finflow.portfolio.application.model

import com.finflow.portfolio.domain.AssetClass
import com.finflow.shared.application.model.MoneyOutput

data class PortfolioPositionResponse(
    val assetCode: String,
    val assetName: String,
    val assetClass: AssetClass,
    val currentValue: MoneyOutput,
)
