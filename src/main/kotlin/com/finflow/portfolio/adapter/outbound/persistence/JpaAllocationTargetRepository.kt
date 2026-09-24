package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.application.port.outbound.AllocationTargetRepository
import com.finflow.portfolio.domain.AllocationTarget
import org.springframework.stereotype.Repository

@Repository
class JpaAllocationTargetRepository(
    private val delegate: SpringDataAllocationTargetRepository,
) : AllocationTargetRepository {
    override fun save(value: AllocationTarget): AllocationTarget = delegate.save(value.toEntity()).toDomain()

    override fun saveAll(values: List<AllocationTarget>): List<AllocationTarget> =
        delegate
            .saveAll(
                values.map {
                    it.toEntity()
                },
            ).map { it.toDomain() }

    override fun findAllByUserId(userId: Int): List<AllocationTarget> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun deleteAllByUserId(userId: Int) {
        delegate.deleteAllByUserId(userId)
        delegate.flush()
    }
}
