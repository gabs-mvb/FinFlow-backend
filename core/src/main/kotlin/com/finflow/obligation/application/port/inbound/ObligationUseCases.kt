package com.finflow.obligation.application.port.inbound

import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.ObligationResponse
import com.finflow.obligation.application.model.UpdateObligationRequest
import java.util.UUID

interface ObligationUseCases {
    fun create(request: CreateObligationRequest): ObligationResponse

    fun update(
        id: UUID,
        request: UpdateObligationRequest,
    ): ObligationResponse

    fun list(): List<ObligationResponse>

    fun markPaid(id: UUID): ObligationResponse
}
