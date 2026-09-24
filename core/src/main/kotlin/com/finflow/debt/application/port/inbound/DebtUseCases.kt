package com.finflow.debt.application.port.inbound

import com.finflow.debt.application.model.CreateDebtRequest
import com.finflow.debt.application.model.DebtResponse
import java.util.UUID

interface DebtUseCases {
    fun create(request: CreateDebtRequest): DebtResponse

    fun list(): List<DebtResponse>

    fun markPaid(id: UUID): DebtResponse
}
