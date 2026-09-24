package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.domain.PortfolioPosition

internal fun PortfolioPositionEntity.toDomain(): PortfolioPosition =
    PortfolioPosition(
        userId = userId,
        id = id,
        assetCode = assetCode,
        assetName = assetName,
        assetClass = assetClass,
        currentValue = currentValue,
        currency = currency,
        updatedAt = updatedAt,
    )

internal fun PortfolioPosition.toEntity(): PortfolioPositionEntity =
    PortfolioPositionEntity(
        userId = userId,
        id = id,
        assetCode = assetCode,
        assetName = assetName,
        assetClass = assetClass,
        currentValue = currentValue,
        currency = currency,
        updatedAt = updatedAt,
    )
