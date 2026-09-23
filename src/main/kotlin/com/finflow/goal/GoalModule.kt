package com.finflow.goal

import com.finflow.audit.AuditService
import com.finflow.shared.api.BusinessRuleException
import com.finflow.shared.api.ResourceNotFoundException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyInput
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

enum class GoalStatus { ACTIVE, ACHIEVED, CANCELLED }

@Entity
@Table(name = "financial_goals")
class GoalEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 160) var name: String = "",
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    var targetAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
    var currentAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "target_date") var targetDate: LocalDate? = null,
    @Column(nullable = false) var priority: Int = 3,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24) var status: GoalStatus = GoalStatus.ACTIVE,
    @Column(name = "created_at", nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface GoalRepository : JpaRepository<GoalEntity, UUID> {
    fun findAllByUserId(userId: Int): List<GoalEntity>
    fun findByIdAndUserId(id: UUID, userId: Int): GoalEntity?
}

data class CreateGoalRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:Valid val targetAmount: MoneyInput,
    @field:Valid val currentAmount: MoneyInput = MoneyInput(BigDecimal.ZERO),
    val targetDate: LocalDate? = null,
    @field:Min(1) @field:Max(5) val priority: Int = 3,
)

data class UpdateGoalProgressRequest(@field:Valid val currentAmount: MoneyInput)

data class GoalResponse(
    val id: UUID,
    val name: String,
    val targetAmount: MoneyOutput,
    val currentAmount: MoneyOutput,
    val targetDate: LocalDate?,
    val priority: Int,
    val status: GoalStatus,
)

@Service
class GoalService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val repository: GoalRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateGoalRequest): GoalResponse {
        val target = request.targetAmount.toMoney()
        val current = request.currentAmount.toMoney()
        require(target.isPositive()) { "A meta deve ser maior que zero" }
        require(target.currency == current.currency) { "Os valores da meta devem usar a mesma moeda" }
        if (current.amount > target.amount) {
            throw BusinessRuleException("O valor atual não pode superar a meta", "GOAL_PROGRESS_INVALID")
        }
        val now = OffsetDateTime.now(clock)
        val saved = repository.save(
            GoalEntity(
                userId = currentUser.id(),
                name = request.name.trim(), targetAmount = target.amount, currentAmount = current.amount,
                currency = target.currency.currencyCode, targetDate = request.targetDate,
                priority = request.priority,
                status = if (current.amount == target.amount) GoalStatus.ACHIEVED else GoalStatus.ACTIVE,
                createdAt = now, updatedAt = now,
            ),
        )
        auditService.record("GOAL_CREATED", "GOAL", saved.id)
        return saved.toResponse()
    }

    @Transactional
    fun list(): List<GoalResponse> = repository.findAllByUserId(currentUser.id()).sortedBy { it.priority }.map { it.toResponse() }

    @Transactional
    fun updateProgress(id: UUID, request: UpdateGoalProgressRequest): GoalResponse {
        val goal = repository.findByIdAndUserId(id, currentUser.id())
            ?: throw ResourceNotFoundException("Meta não encontrada")
        val current = request.currentAmount.toMoney()
        require(current.currency.currencyCode == goal.currency) { "A moeda da meta não pode ser alterada" }
        if (current.amount > goal.targetAmount) {
            throw BusinessRuleException("O valor atual não pode superar a meta", "GOAL_PROGRESS_INVALID")
        }
        goal.currentAmount = current.amount
        goal.status = if (current.amount == goal.targetAmount) GoalStatus.ACHIEVED else GoalStatus.ACTIVE
        goal.updatedAt = OffsetDateTime.now(clock)
        auditService.record("GOAL_PROGRESS_UPDATED", "GOAL", id)
        return repository.save(goal).toResponse()
    }
}

@RestController
@RequestMapping("/api/v1/goals")
class GoalController(private val service: GoalService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateGoalRequest): GoalResponse = service.create(request)

    @GetMapping fun list(): List<GoalResponse> = service.list()

    @PatchMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateGoalProgressRequest,
    ): GoalResponse = service.updateProgress(id, request)
}

private fun GoalEntity.toResponse(): GoalResponse {
    val parsedCurrency = Currency.getInstance(currency)
    return GoalResponse(
        id, name,
        Money(targetAmount, parsedCurrency).toOutput(),
        Money(currentAmount, parsedCurrency).toOutput(),
        targetDate, priority, status,
    )
}
