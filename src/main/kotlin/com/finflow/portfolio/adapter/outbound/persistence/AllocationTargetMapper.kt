package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.domain.AllocationTarget

internal fun AllocationTargetEntity.toDomain(): AllocationTarget =
    AllocationTarget(
        userId = userId,
        id = id,
        assetClass = assetClass,
        targetPercentage = targetPercentage,
        minimumPercentage = minimumPercentage,
        maximumPercentage = maximumPercentage,
    )

internal fun AllocationTarget.toEntity(): AllocationTargetEntity =
    AllocationTargetEntity(
        userId = userId,
        id = id,
        assetClass = assetClass,
        targetPercentage = targetPercentage,
        minimumPercentage = minimumPercentage,
        maximumPercentage = maximumPercentage,
    )
