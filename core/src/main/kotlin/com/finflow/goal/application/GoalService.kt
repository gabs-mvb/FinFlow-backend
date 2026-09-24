package com.finflow.goal.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.goal.application.model.CreateGoalRequest
import com.finflow.goal.application.model.GoalResponse
import com.finflow.goal.application.model.UpdateGoalProgressRequest
import com.finflow.goal.application.port.inbound.GoalUseCases
import com.finflow.goal.application.port.outbound.GoalRepository
import com.finflow.goal.domain.Goal
import com.finflow.goal.domain.GoalStatus
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.BusinessRuleException
import com.finflow.shared.domain.ResourceNotFoundException
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

class GoalService(
    private val currentUser: CurrentUser,
    private val repository: GoalRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : GoalUseCases {
    override fun create(request: CreateGoalRequest): GoalResponse {
        val target = request.targetAmount.toMoney()
        val current = request.currentAmount.toMoney()
        require(target.isPositive()) { "A meta deve ser maior que zero" }
        require(target.currency == current.currency) { "Os valores da meta devem usar a mesma moeda" }
        if (current.amount > target.amount) {
            throw BusinessRuleException("O valor atual não pode superar a meta", "GOAL_PROGRESS_INVALID")
        }
        val now = OffsetDateTime.now(clock)
        val saved =
            repository.save(
                Goal(
                    userId = currentUser.id(),
                    name = request.name.trim(),
                    targetAmount = target.amount,
                    currentAmount = current.amount,
                    currency = target.currency.currencyCode,
                    targetDate = request.targetDate,
                    priority = request.priority,
                    status = if (current.amount == target.amount) GoalStatus.ACHIEVED else GoalStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        auditService.record("GOAL_CREATED", "GOAL", saved.id)
        return saved.toResponse()
    }

    override fun list(): List<GoalResponse> = repository.findAllByUserId(currentUser.id()).sortedBy { it.priority }.map { it.toResponse() }

    override fun updateProgress(
        id: UUID,
        request: UpdateGoalProgressRequest,
    ): GoalResponse {
        val goal =
            repository.findByIdAndUserId(id, currentUser.id())
                ?: throw ResourceNotFoundException("Meta não encontrada")
        val current = request.currentAmount.toMoney()
        require(current.currency.currencyCode == goal.currency) { "A moeda da meta não pode ser alterada" }
        if (current.amount > goal.targetAmount) {
            throw BusinessRuleException("O valor atual não pode superar a meta", "GOAL_PROGRESS_INVALID")
        }
        val updated =
            goal.copy(
                currentAmount = current.amount,
                status = if (current.amount == goal.targetAmount) GoalStatus.ACHIEVED else GoalStatus.ACTIVE,
                updatedAt = OffsetDateTime.now(clock),
            )
        auditService.record("GOAL_PROGRESS_UPDATED", "GOAL", id)
        return repository.save(updated).toResponse()
    }
}
