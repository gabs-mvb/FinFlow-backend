package com.finflow.obligation.application.model

import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import com.finflow.shared.application.model.MoneyOutput
import java.time.LocalDate
import java.util.UUID

data class ObligationResponse(
    val id: UUID,
    val name: String,
    val type: ObligationType,
    val amount: MoneyOutput,
    val dueDate: LocalDate,
    val recurring: Boolean,
    val dueDay: Int?,
    val status: ObligationStatus,
)
