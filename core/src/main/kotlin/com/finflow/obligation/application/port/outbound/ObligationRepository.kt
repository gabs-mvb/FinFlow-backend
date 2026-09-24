package com.finflow.obligation.application.port.outbound

import com.finflow.obligation.domain.Obligation
import com.finflow.obligation.domain.ObligationStatus
import java.time.LocalDate
import java.util.UUID

interface ObligationRepository {
    fun save(value: Obligation): Obligation

    fun findAllByUserId(userId: Int): List<Obligation>

    fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ): Obligation?

    fun findAllByUserIdAndStatusAndDueDateBetween(
        userId: Int,
        status: ObligationStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<Obligation>
}
