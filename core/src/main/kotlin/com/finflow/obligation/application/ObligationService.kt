package com.finflow.obligation.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.ObligationResponse
import com.finflow.obligation.application.model.UpdateObligationRequest
import com.finflow.obligation.application.port.inbound.ObligationUseCases
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.Obligation
import com.finflow.obligation.domain.ObligationStatus
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ResourceNotFoundException
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

class ObligationService(
    private val currentUser: CurrentUser,
    private val repository: ObligationRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : ObligationUseCases {
    override fun create(request: CreateObligationRequest): ObligationResponse {
        val money = request.amount.toMoney()
        require(money.isPositive()) { "O valor da obrigação deve ser maior que zero" }
        val now = OffsetDateTime.now(clock)
        val saved =
            repository.save(
                Obligation(
                    userId = currentUser.id(),
                    name = request.name.trim(),
                    obligationType = request.type,
                    amount = money.amount,
                    currency = money.currency.currencyCode,
                    dueDate = request.dueDate,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        auditService.record("OBLIGATION_CREATED", "OBLIGATION", saved.id)
        return saved.toResponse()
    }

    override fun update(
        id: UUID,
        request: UpdateObligationRequest,
    ): ObligationResponse {
        val obligation =
            repository.findByIdAndUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Obrigação não encontrada")
        val money = request.amount.toMoney()
        val saved =
            repository.save(
                obligation.copy(
                    name = request.name.trim(),
                    obligationType = request.type,
                    amount = money.amount,
                    currency = money.currency.currencyCode,
                    dueDate = request.dueDate,
                    status = request.status,
                    updatedAt = OffsetDateTime.now(clock),
                ),
            )
        auditService.record("OBLIGATION_UPDATED", "OBLIGATION", id)
        return saved.toResponse()
    }

    override fun list(): List<ObligationResponse> =
        repository
            .findAllByUserId(currentUser.id())
            .sortedBy { it.dueDate }
            .map { it.toResponse() }

    override fun markPaid(id: UUID): ObligationResponse {
        val obligation =
            repository.findByIdAndUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Obrigação não encontrada")
        val updated =
            obligation.copy(
                status = ObligationStatus.PAID,
                updatedAt = OffsetDateTime.now(clock),
            )
        auditService.record("OBLIGATION_PAID", "OBLIGATION", id)
        return repository.save(updated).toResponse()
    }
}
