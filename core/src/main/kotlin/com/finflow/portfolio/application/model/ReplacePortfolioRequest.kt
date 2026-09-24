package com.finflow.portfolio.application.model

data class ReplacePortfolioRequest(
    val positions: List<PortfolioPositionInput>,
) {
    init {
        require(positions.size in 0..500) { "Tamanho inválido para positions" }
    }
}
