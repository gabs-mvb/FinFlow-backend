package com.finflow.obligation.adapter.outbound.persistence

import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.Obligation
import com.finflow.obligation.domain.ObligationStatus
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class JpaObligationRepository(
    private val delegate: SpringDataObligationRepository,
) : ObligationRepository {
    override fun save(value: Obligation): Obligation = delegate.save(value.toEntity()).toDomain()

    override fun findAllByUserId(userId: Int): List<Obligation> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Obligation? = delegate.findByIdAndUserId(id, userId)?.toDomain()

    override fun findAllByUserIdAndStatusAndDueDateBetween(
        userId: Int,
        status: ObligationStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<Obligation> = delegate.findAllByUserIdAndStatusAndDueDateBetween(userId, status, from, to).map { it.toDomain() }
}
