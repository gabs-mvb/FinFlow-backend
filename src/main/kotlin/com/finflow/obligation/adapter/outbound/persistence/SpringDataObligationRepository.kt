package com.finflow.obligation.adapter.outbound.persistence

import com.finflow.obligation.domain.ObligationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface SpringDataObligationRepository : JpaRepository<ObligationEntity, UUID> {
    fun findAllByUserId(userId: Int): List<ObligationEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): ObligationEntity?

    fun findAllByUserIdAndStatusAndDueDateBetween(
        userId: Int,
        status: ObligationStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<ObligationEntity>
}
