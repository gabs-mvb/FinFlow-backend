package com.finflow.debt.adapter.outbound.persistence

import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.debt.domain.Debt
import com.finflow.debt.domain.DebtStatus
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaDebtRepository(
    private val delegate: SpringDataDebtRepository,
) : DebtRepository {
    override fun save(value: Debt): Debt = delegate.save(value.toEntity()).toDomain()

    override fun findAllByUserId(userId: Int): List<Debt> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Debt? = delegate.findByIdAndUserId(id, userId)?.toDomain()

    override fun findAllByUserIdAndStatus(
        userId: Int,
        status: DebtStatus,
    ): List<Debt> =
        delegate.findAllByUserIdAndStatus(userId, status).map {
            it.toDomain()
        }
}
