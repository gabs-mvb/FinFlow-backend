package com.finflow.obligation.application.port.inbound

import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.ObligationResponse
import java.util.UUID

interface ObligationUseCases {
    fun create(request: CreateObligationRequest): ObligationResponse

    fun list(): List<ObligationResponse>

    fun markPaid(id: UUID): ObligationResponse
}
