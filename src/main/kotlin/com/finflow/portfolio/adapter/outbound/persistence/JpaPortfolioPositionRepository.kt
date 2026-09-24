package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.application.port.outbound.PortfolioPositionRepository
import com.finflow.portfolio.domain.PortfolioPosition
import org.springframework.stereotype.Repository

@Repository
class JpaPortfolioPositionRepository(
    private val delegate: SpringDataPortfolioPositionRepository,
) : PortfolioPositionRepository {
    override fun save(value: PortfolioPosition): PortfolioPosition = delegate.save(value.toEntity()).toDomain()

    override fun saveAll(values: List<PortfolioPosition>): List<PortfolioPosition> =
        delegate
            .saveAll(
                values.map {
                    it.toEntity()
                },
            ).map { it.toDomain() }

    override fun findAllByUserId(userId: Int): List<PortfolioPosition> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun deleteAllByUserId(userId: Int) {
        delegate.deleteAllByUserId(userId)
        delegate.flush()
    }
}
