package com.finflow.portfolio.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataPortfolioPositionRepository : JpaRepository<PortfolioPositionEntity, UUID> {
    fun findAllByUserId(userId: Int): List<PortfolioPositionEntity>

    fun deleteAllByUserId(userId: Int)
}
