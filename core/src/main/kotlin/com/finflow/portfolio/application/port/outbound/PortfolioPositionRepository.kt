package com.finflow.portfolio.application.port.outbound

import com.finflow.portfolio.domain.PortfolioPosition

interface PortfolioPositionRepository {
    fun save(value: PortfolioPosition): PortfolioPosition

    fun saveAll(values: List<PortfolioPosition>): List<PortfolioPosition>

    fun findAllByUserId(userId: Int): List<PortfolioPosition>

    fun deleteAllByUserId(userId: Int)
}
