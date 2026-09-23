package com.finflow.obligation

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

enum class ObligationType {
    CREDIT_CARD,
    VEHICLE,
    HOUSING,
    UTILITIES,
    TAX,
    SUBSCRIPTION,
    OTHER,
}

enum class ObligationStatus {
    PENDING,
    PAID,
    CANCELLED,
}

@Entity
@Table(name = "obligations")
class ObligationEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 160)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 40)
    var obligationType: ObligationType = ObligationType.OTHER,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3)
    var currency: String = "BRL",
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var status: ObligationStatus = ObligationStatus.PENDING,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface ObligationRepository : JpaRepository<ObligationEntity, UUID> {
    fun findAllByUserId(userId: Int): List<ObligationEntity>
    fun findByIdAndUserId(id: UUID, userId: Int): ObligationEntity?
    fun findAllByUserIdAndStatusAndDueDateBetween(
        userId: Int,
        status: ObligationStatus,
        from: LocalDate,
        to: LocalDate,
    ): List<ObligationEntity>
}

data class CreateObligationRequest(
    @field:NotBlank @field:Size(max = 160)
    val name: String,
    val type: ObligationType,
    @field:Valid val amount: MoneyInput,
    val dueDate: LocalDate,
)

data class ObligationResponse(
    val id: UUID,
    val name: String,
    val type: ObligationType,
    val amount: MoneyOutput,
    val dueDate: LocalDate,
    val status: ObligationStatus,
)

@Service
class ObligationService(
    private val currentUser: com.finflow.shared.security.CurrentUser,
    private val repository: ObligationRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateObligationRequest): ObligationResponse {
        val money = request.amount.toMoney()
        require(money.isPositive()) { "O valor da obrigação deve ser maior que zero" }
        val now = OffsetDateTime.now(clock)
        val saved = repository.save(
            ObligationEntity(
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

    @Transactional
    fun list(): List<ObligationResponse> = repository.findAllByUserId(currentUser.id())
        .sortedBy { it.dueDate }
        .map { it.toResponse() }

    @Transactional
    fun markPaid(id: UUID): ObligationResponse {
        val obligation = repository.findByIdAndUserId(id, currentUser.id())
            ?: throw ResourceNotFoundException("Obrigação não encontrada")
        obligation.status = ObligationStatus.PAID
        obligation.updatedAt = OffsetDateTime.now(clock)
        auditService.record("OBLIGATION_PAID", "OBLIGATION", id)
        return repository.save(obligation).toResponse()
    }
}

@RestController
@RequestMapping("/api/v1/obligations")
class ObligationController(
    private val service: ObligationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateObligationRequest): ObligationResponse = service.create(request)

    @GetMapping
    fun list(): List<ObligationResponse> = service.list()

    @PatchMapping("/{id}/paid")
    fun markPaid(@PathVariable id: UUID): ObligationResponse = service.markPaid(id)
}

private fun ObligationEntity.toResponse(): ObligationResponse = ObligationResponse(
    id = id,
    name = name,
    type = obligationType,
    amount = Money(amount, Currency.getInstance(currency)).toOutput(),
    dueDate = dueDate,
    status = status,
)

