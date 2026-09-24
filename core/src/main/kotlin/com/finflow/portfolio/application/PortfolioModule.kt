package com.finflow.portfolio.application

import com.finflow.portfolio.application.model.AllocationTargetResponse
import com.finflow.portfolio.application.model.PortfolioPositionResponse
import com.finflow.portfolio.domain.AllocationTarget
import com.finflow.portfolio.domain.PortfolioPosition
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.domain.Money
import java.util.Currency

internal fun PortfolioPosition.toResponse(): PortfolioPositionResponse =
    PortfolioPositionResponse(
        assetCode,
        assetName,
        assetClass,
        Money(currentValue, Currency.getInstance(currency)).toOutput(),
    )

internal fun AllocationTarget.toResponse(): AllocationTargetResponse =
    AllocationTargetResponse(
        assetClass,
        targetPercentage,
        minimumPercentage,
        maximumPercentage,
    )
