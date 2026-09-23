package com.finflow.debt

import com.finflow.audit.AuditService
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
import jakarta.validation.constraints.DecimalMin
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
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

enum class DebtType { CREDIT_CARD, OVERDRAFT, PERSONAL_LOAN, VEHICLE_FINANCING, MORTGAGE, OTHER }
enum class DebtPriority { HIGH_COST, REGULAR }
enum class DebtStatus { ACTIVE, PAID, RENEGOTIATED }

@Entity
@Table(name = "debts")
class DebtEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 160) var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "debt_type", nullable = false, length = 40)
    var debtType: DebtType = DebtType.OTHER,
    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 2)
    var outstandingAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "monthly_payment", nullable = false, precision = 19, scale = 2)
    var monthlyPayment: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "annual_effective_rate", nullable = false, precision = 9, scale = 6)
    var annualEffectiveRate: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24) var priority: DebtPriority = DebtPriority.REGULAR,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24) var status: DebtStatus = DebtStatus.ACTIVE,
    @Column(name = "created_at", nullable = false) var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface DebtRepository : JpaRepository<DebtEntity, UUID> {
    fun findAllByUserId(userId: Int): List<DebtEntity>
    fun findByIdAndUserId(id: UUID, userId: Int): DebtEntity?
    fun findAllByUserIdAndStatus(userId: Int, status: DebtStatus): List<DebtEntity>
}

data class CreateDebtRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    val type: DebtType,
    @field:Valid val outstandingAmount: MoneyInput,
    @field:Valid val monthlyPayment: MoneyInput,
    @field:DecimalMin("0.0") val annualEffectiveRate: BigDecimal,
    val priority: DebtPriority,
)

data class DebtResponse(
    val id: UUID,
    val name: String,
    val type: DebtType,
    val outstandingAmount: MoneyOutput,
    val monthlyPayment: MoneyOutput,
    val annualEffectiveRate: BigDecimal,
    val priority: DebtPriority,
    val status: DebtStatus,
)

@Service
class DebtService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val repository: DebtRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateDebtRequest): DebtResponse {
        val outstanding = request.outstandingAmount.toMoney()
        val payment = request.monthlyPayment.toMoney()
        require(outstanding.isPositive()) { "O saldo da dívida deve ser maior que zero" }
        require(outstanding.currency == payment.currency) { "Os valores da dívida devem usar a mesma moeda" }
        val now = OffsetDateTime.now(clock)
        val saved = repository.save(
            DebtEntity(
                userId = currentUser.id(),
                name = request.name.trim(), debtType = request.type,
                outstandingAmount = outstanding.amount, monthlyPayment = payment.amount,
                currency = outstanding.currency.currencyCode,
                annualEffectiveRate = request.annualEffectiveRate, priority = request.priority,
                createdAt = now, updatedAt = now,
            ),
        )
        auditService.record("DEBT_CREATED", "DEBT", saved.id)
        return saved.toResponse()
    }

    @Transactional
    fun list(): List<DebtResponse> = repository.findAllByUserId(currentUser.id()).map { it.toResponse() }

    @Transactional
    fun markPaid(id: UUID): DebtResponse {
        val debt = repository.findByIdAndUserId(id, currentUser.id())
            ?: throw ResourceNotFoundException("Dívida não encontrada")
        debt.status = DebtStatus.PAID
        debt.outstandingAmount = BigDecimal.ZERO.setScale(2)
        debt.updatedAt = OffsetDateTime.now(clock)
        auditService.record("DEBT_PAID", "DEBT", id)
        return repository.save(debt).toResponse()
    }
}

@RestController
@RequestMapping("/api/v1/debts")
class DebtController(private val service: DebtService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateDebtRequest): DebtResponse = service.create(request)

    @GetMapping fun list(): List<DebtResponse> = service.list()

    @PatchMapping("/{id}/paid")
    fun markPaid(@PathVariable id: UUID): DebtResponse = service.markPaid(id)
}

private fun DebtEntity.toResponse(): DebtResponse {
    val parsedCurrency = Currency.getInstance(currency)
    return DebtResponse(
        id, name, debtType,
        Money(outstandingAmount, parsedCurrency).toOutput(),
        Money(monthlyPayment, parsedCurrency).toOutput(),
        annualEffectiveRate, priority, status,
    )
}

