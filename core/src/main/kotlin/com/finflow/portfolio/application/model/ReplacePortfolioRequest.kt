package com.finflow.portfolio.application.model

data class ReplacePortfolioRequest(
    val positions: List<PortfolioPositionInput>,
    val targets: List<AllocationTargetInput>,
) {
    init {
        require(positions.size in 0..500) { "Tamanho inválido para positions" }
        require(targets.size in 1..20) { "Tamanho inválido para targets" }
    }
}
