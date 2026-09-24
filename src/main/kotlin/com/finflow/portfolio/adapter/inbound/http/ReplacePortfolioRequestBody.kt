package com.finflow.portfolio.adapter.inbound.http

import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class ReplacePortfolioRequestBody(
    @field:Valid @field:Size(max = 500) val positions: List<PortfolioPositionInputBody>,
    @field:Valid @field:Size(min = 1, max = 20) val targets: List<AllocationTargetInputBody>,
)

fun ReplacePortfolioRequestBody.toCommand(): ReplacePortfolioRequest =
    ReplacePortfolioRequest(
        positions = positions.map { it.toCommand() },
        targets = targets.map { it.toCommand() },
    )
