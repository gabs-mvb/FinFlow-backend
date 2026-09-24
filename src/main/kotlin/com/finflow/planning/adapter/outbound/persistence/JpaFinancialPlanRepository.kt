package com.finflow.planning.adapter.outbound.persistence

import com.finflow.authentication.adapter.outbound.persistence.UserEntity
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.domain.FinancialPlan
import com.finflow.shared.domain.ResourceNotFoundException
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaFinancialPlanRepository(
    private val delegate: SpringDataFinancialPlanRepository,
    private val entityManager: EntityManager,
) : FinancialPlanRepository {
    override fun lockChangesForUser(userId: Int) {
        entityManager.find(UserEntity::class.java, userId, LockModeType.PESSIMISTIC_WRITE)
            ?: throw ResourceNotFoundException("Usuário não encontrado")
    }

    override fun save(value: FinancialPlan): FinancialPlan = delegate.save(value.toEntity()).toDomain()

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): FinancialPlan? = delegate.findByIdAndUserId(id, userId)?.toDomain()

    override fun findFirstByUserIdOrderByGeneratedAtDesc(userId: Int): FinancialPlan? =
        delegate.findFirstByUserIdOrderByGeneratedAtDesc(userId)?.toDomain()
}
