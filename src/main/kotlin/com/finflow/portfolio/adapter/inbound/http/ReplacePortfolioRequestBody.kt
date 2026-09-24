package com.finflow.portfolio.adapter.inbound.http

import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class ReplacePortfolioRequestBody(
    @field:Valid @field:Size(max = 500) val positions: List<PortfolioPositionInputBody>,
)

fun ReplacePortfolioRequestBody.toCommand(): ReplacePortfolioRequest =
    ReplacePortfolioRequest(
        positions = positions.map { it.toCommand() },
    )
