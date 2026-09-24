package com.finflow.portfolio.application.model

import com.finflow.portfolio.domain.AssetClass
import com.finflow.shared.application.model.MoneyInput

data class PortfolioPositionInput(
    val assetCode: String,
    val assetName: String,
    val assetClass: AssetClass,
    val currentValue: MoneyInput,
) {
    init {
        require(assetCode.isNotBlank()) { "assetCode não pode estar vazio" }
        require(assetCode.length in 0..48) { "Tamanho inválido para assetCode" }
        require(assetName.isNotBlank()) { "assetName não pode estar vazio" }
        require(assetName.length in 0..160) { "Tamanho inválido para assetName" }
    }
}
