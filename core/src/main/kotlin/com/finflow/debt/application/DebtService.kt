package com.finflow.debt.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.debt.application.model.CreateDebtRequest
import com.finflow.debt.application.model.DebtResponse
import com.finflow.debt.application.port.inbound.DebtUseCases
import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.debt.domain.Debt
import com.finflow.debt.domain.DebtStatus
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ResourceNotFoundException
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

class DebtService(
    private val currentUser: CurrentUser,
    private val repository: DebtRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : DebtUseCases {
    override fun create(request: CreateDebtRequest): DebtResponse {
        val outstanding = request.outstandingAmount.toMoney()
        val payment = request.monthlyPayment.toMoney()
        require(outstanding.isPositive()) { "O saldo da dívida deve ser maior que zero" }
        require(outstanding.currency == payment.currency) { "Os valores da dívida devem usar a mesma moeda" }
        val now = OffsetDateTime.now(clock)
        val saved =
            repository.save(
                Debt(
                    userId = currentUser.id(),
                    name = request.name.trim(),
                    debtType = request.type,
                    outstandingAmount = outstanding.amount,
                    monthlyPayment = payment.amount,
                    currency = outstanding.currency.currencyCode,
                    annualEffectiveRate = request.annualEffectiveRate,
                    priority = request.priority,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        auditService.record("DEBT_CREATED", "DEBT", saved.id)
        return saved.toResponse()
    }

    override fun list(): List<DebtResponse> = repository.findAllByUserId(currentUser.id()).map { it.toResponse() }

    override fun markPaid(id: UUID): DebtResponse {
        val debt =
            repository.findByIdAndUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Dívida não encontrada")
        val updated =
            debt.copy(
                status = DebtStatus.PAID,
                outstandingAmount = BigDecimal.ZERO.setScale(2),
                updatedAt = OffsetDateTime.now(clock),
            )
        auditService.record("DEBT_PAID", "DEBT", id)
        return repository.save(updated).toResponse()
    }
}
