package com.finflow.portfolio.adapter.inbound.http

import com.finflow.portfolio.application.model.PortfolioPositionInput
import com.finflow.portfolio.domain.AssetClass
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PortfolioPositionInputBody(
    @field:NotBlank @field:Size(max = 48) val assetCode: String,
    @field:NotBlank @field:Size(max = 160) val assetName: String,
    val assetClass: AssetClass,
    @field:Valid val currentValue: MoneyInputBody,
)

fun PortfolioPositionInputBody.toCommand(): PortfolioPositionInput =
    PortfolioPositionInput(
        assetCode = assetCode,
        assetName = assetName,
        assetClass = assetClass,
        currentValue = currentValue.toCommand(),
    )
