package com.finflow.portfolio.application.model

import com.finflow.portfolio.domain.AssetClass
import com.finflow.shared.application.model.MoneyOutput

data class ContributionAllocation(
    val assetClass: AssetClass,
    val amount: MoneyOutput,
    val reason: String,
)
