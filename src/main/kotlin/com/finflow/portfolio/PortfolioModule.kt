package com.finflow.portfolio

import com.finflow.audit.AuditService
import com.finflow.shared.api.BusinessRuleException
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
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

enum class AssetClass {
    CASH,
    FIXED_INCOME,
    BRAZILIAN_EQUITY,
    INTERNATIONAL_EQUITY,
    REAL_ESTATE_FUND,
    ETF,
    CRYPTO,
    PENSION,
    ALTERNATIVE,
}

@Entity
@Table(name = "portfolio_positions")
class PortfolioPositionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "asset_code", nullable = false, unique = true, length = 48)
    var assetCode: String = "",
    @Column(name = "asset_name", nullable = false, length = 160)
    var assetName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 40)
    var assetClass: AssetClass = AssetClass.CASH,
    @Column(name = "current_value", nullable = false, precision = 19, scale = 2)
    var currentValue: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "allocation_targets")
class AllocationTargetEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, unique = true, length = 40)
    var assetClass: AssetClass = AssetClass.CASH,
    @Column(name = "target_percentage", nullable = false, precision = 7, scale = 4)
    var targetPercentage: BigDecimal = BigDecimal.ZERO,
    @Column(name = "minimum_percentage", nullable = false, precision = 7, scale = 4)
    var minimumPercentage: BigDecimal = BigDecimal.ZERO,
    @Column(name = "maximum_percentage", nullable = false, precision = 7, scale = 4)
    var maximumPercentage: BigDecimal = BigDecimal.ZERO,
)

interface PortfolioPositionRepository : JpaRepository<PortfolioPositionEntity, UUID>
interface AllocationTargetRepository : JpaRepository<AllocationTargetEntity, UUID>

data class PortfolioPositionInput(
    @field:NotBlank @field:Size(max = 48) val assetCode: String,
    @field:NotBlank @field:Size(max = 160) val assetName: String,
    val assetClass: AssetClass,
    @field:Valid val currentValue: MoneyInput,
)

data class AllocationTargetInput(
    val assetClass: AssetClass,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val targetPercentage: BigDecimal,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val minimumPercentage: BigDecimal,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val maximumPercentage: BigDecimal,
)

data class ReplacePortfolioRequest(
    @field:Valid @field:Size(max = 500) val positions: List<PortfolioPositionInput>,
    @field:Valid @field:Size(min = 1, max = 20) val targets: List<AllocationTargetInput>,
)

data class PortfolioPositionResponse(
    val assetCode: String,
    val assetName: String,
    val assetClass: AssetClass,
    val currentValue: MoneyOutput,
)

data class AllocationTargetResponse(
    val assetClass: AssetClass,
    val targetPercentage: BigDecimal,
    val minimumPercentage: BigDecimal,
    val maximumPercentage: BigDecimal,
)

data class PortfolioResponse(
    val positions: List<PortfolioPositionResponse>,
    val targets: List<AllocationTargetResponse>,
)

data class ContributionAllocation(
    val assetClass: AssetClass,
    val amount: MoneyOutput,
    val reason: String,
)

@Service
class PortfolioService(
    private val positionRepository: PortfolioPositionRepository,
    private val targetRepository: AllocationTargetRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun replace(request: ReplacePortfolioRequest): PortfolioResponse {
        validateTargets(request.targets)
        val duplicateAssets = request.positions.groupingBy { it.assetCode.uppercase() }.eachCount()
            .filterValues { it > 1 }.keys
        if (duplicateAssets.isNotEmpty()) {
            throw BusinessRuleException("Ativos duplicados: $duplicateAssets", "DUPLICATE_PORTFOLIO_ASSET")
        }
        val currencies = request.positions.map { it.currentValue.toMoney().currency.currencyCode }.distinct()
        if (currencies.size > 1) {
            throw BusinessRuleException(
                "O MVP exige posições consolidadas em uma única moeda",
                "PORTFOLIO_CURRENCY_MISMATCH",
            )
        }
        val now = OffsetDateTime.now(clock)
        positionRepository.deleteAllInBatch()
        targetRepository.deleteAllInBatch()
        positionRepository.saveAll(request.positions.map { input ->
            val value = input.currentValue.toMoney()
            PortfolioPositionEntity(
                assetCode = input.assetCode.trim().uppercase(),
                assetName = input.assetName.trim(),
                assetClass = input.assetClass,
                currentValue = value.amount,
                currency = value.currency.currencyCode,
                updatedAt = now,
            )
        })
        targetRepository.saveAll(request.targets.map { input ->
            AllocationTargetEntity(
                assetClass = input.assetClass,
                targetPercentage = input.targetPercentage,
                minimumPercentage = input.minimumPercentage,
                maximumPercentage = input.maximumPercentage,
            )
        })
        auditService.record("PORTFOLIO_REPLACED", "PORTFOLIO", details = "positions=${request.positions.size}")
        return get()
    }

    @Transactional
    fun get(): PortfolioResponse = PortfolioResponse(
        positions = positionRepository.findAll().map { it.toResponse() },
        targets = targetRepository.findAll().map { it.toResponse() },
    )

    /**
     * Divide um novo aporte pelos maiores desvios das metas, sem vender
     * posições existentes nem ultrapassar o valor recebido.
     */
    @Transactional
    fun allocateContribution(contribution: Money): List<ContributionAllocation> {
        if (!contribution.isPositive()) return emptyList()
        val targets = targetRepository.findAll()
        if (targets.isEmpty()) return emptyList()
        val positions = positionRepository.findAll()
        positions.forEach { position ->
            require(position.currency == contribution.currency.currencyCode) {
                "A carteira deve estar consolidada na moeda do aporte"
            }
        }
        val currentByClass = positions.groupBy { it.assetClass }
            .mapValues { (_, values) -> values.fold(BigDecimal.ZERO) { acc, item -> acc + item.currentValue } }
        val currentTotal = currentByClass.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val postContributionTotal = currentTotal + contribution.amount
        val gaps = targets.associateWith { target ->
            val desired = postContributionTotal.multiply(target.targetPercentage)
                .divide(BigDecimal("100"), 8, RoundingMode.HALF_EVEN)
            (desired - currentByClass.getOrDefault(target.assetClass, BigDecimal.ZERO)).max(BigDecimal.ZERO)
        }
        val gapTotal = gaps.values.fold(BigDecimal.ZERO, BigDecimal::add)
        if (gapTotal.compareTo(BigDecimal.ZERO) == 0) return emptyList()

        val allocations = gaps.filterValues { it > BigDecimal.ZERO }
            .map { (target, gap) ->
                target.assetClass to contribution.amount.multiply(gap)
                    .divide(gapTotal, 2, RoundingMode.HALF_EVEN)
            }.toMutableList()
        val allocated = allocations.fold(BigDecimal.ZERO) { acc, (_, amount) -> acc + amount }
        val roundingDifference = contribution.amount - allocated
        if (allocations.isNotEmpty()) {
            val first = allocations.first()
            allocations[0] = first.first to (first.second + roundingDifference)
        }
        return allocations.map { (assetClass, amount) ->
            ContributionAllocation(
                assetClass = assetClass,
                amount = Money(amount, contribution.currency).toOutput(),
                reason = "Classe abaixo da alocação-alvo; aporte direcionado sem vender posições",
            )
        }
    }

    private fun validateTargets(targets: List<AllocationTargetInput>) {
        if (targets.map { it.assetClass }.distinct().size != targets.size) {
            throw BusinessRuleException("Cada classe deve possuir apenas uma meta", "DUPLICATE_ALLOCATION_TARGET")
        }
        targets.forEach { target ->
            if (target.minimumPercentage > target.targetPercentage ||
                target.targetPercentage > target.maximumPercentage
            ) {
                throw BusinessRuleException(
                    "A meta de ${target.assetClass} deve ficar entre mínimo e máximo",
                    "INVALID_ALLOCATION_BAND",
                )
            }
        }
        val total = targets.fold(BigDecimal.ZERO) { acc, target -> acc + target.targetPercentage }
        if (total.compareTo(BigDecimal("100")) != 0) {
            throw BusinessRuleException("As alocações-alvo devem somar 100%", "ALLOCATION_SUM_INVALID")
        }
    }
}

@RestController
@RequestMapping("/api/v1/portfolio")
class PortfolioController(private val service: PortfolioService) {
    @PutMapping
    fun replace(@Valid @RequestBody request: ReplacePortfolioRequest): PortfolioResponse = service.replace(request)

    @GetMapping
    fun get(): PortfolioResponse = service.get()
}

private fun PortfolioPositionEntity.toResponse(): PortfolioPositionResponse = PortfolioPositionResponse(
    assetCode, assetName, assetClass,
    Money(currentValue, Currency.getInstance(currency)).toOutput(),
)

private fun AllocationTargetEntity.toResponse(): AllocationTargetResponse = AllocationTargetResponse(
    assetClass, targetPercentage, minimumPercentage, maximumPercentage,
)
