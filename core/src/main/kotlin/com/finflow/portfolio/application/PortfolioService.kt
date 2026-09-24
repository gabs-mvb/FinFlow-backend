package com.finflow.portfolio.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.portfolio.application.model.AllocationTargetInput
import com.finflow.portfolio.application.model.ContributionAllocation
import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.portfolio.application.port.outbound.AllocationTargetRepository
import com.finflow.portfolio.application.port.outbound.PortfolioPositionRepository
import com.finflow.portfolio.domain.AllocationTarget
import com.finflow.portfolio.domain.PortfolioPosition
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.BusinessRuleException
import com.finflow.shared.domain.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.OffsetDateTime

class PortfolioService(
    private val currentUser: CurrentUser,
    private val positionRepository: PortfolioPositionRepository,
    private val targetRepository: AllocationTargetRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : PortfolioUseCases {
    override fun replace(request: ReplacePortfolioRequest): PortfolioResponse {
        validateTargets(request.targets)
        val duplicateAssets =
            request.positions
                .groupingBy { it.assetCode.uppercase() }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        if (duplicateAssets.isNotEmpty()) {
            throw BusinessRuleException("Ativos duplicados: $duplicateAssets", "DUPLICATE_PORTFOLIO_ASSET")
        }
        val currencies =
            request.positions
                .map {
                    it.currentValue
                        .toMoney()
                        .currency.currencyCode
                }.distinct()
        if (currencies.size > 1) {
            throw BusinessRuleException(
                "O MVP exige posições consolidadas em uma única moeda",
                "PORTFOLIO_CURRENCY_MISMATCH",
            )
        }
        val now = OffsetDateTime.now(clock)
        positionRepository.deleteAllByUserId(currentUser.id())
        targetRepository.deleteAllByUserId(currentUser.id())
        positionRepository.saveAll(
            request.positions.map { input ->
                val value = input.currentValue.toMoney()
                PortfolioPosition(
                    userId = currentUser.id(),
                    assetCode = input.assetCode.trim().uppercase(),
                    assetName = input.assetName.trim(),
                    assetClass = input.assetClass,
                    currentValue = value.amount,
                    currency = value.currency.currencyCode,
                    updatedAt = now,
                )
            },
        )
        targetRepository.saveAll(
            request.targets.map { input ->
                AllocationTarget(
                    userId = currentUser.id(),
                    assetClass = input.assetClass,
                    targetPercentage = input.targetPercentage,
                    minimumPercentage = input.minimumPercentage,
                    maximumPercentage = input.maximumPercentage,
                )
            },
        )
        auditService.record("PORTFOLIO_REPLACED", "PORTFOLIO", details = "positions=${request.positions.size}")
        return get()
    }

    override fun get(): PortfolioResponse =
        PortfolioResponse(
            positions = positionRepository.findAllByUserId(currentUser.id()).map { it.toResponse() },
            targets = targetRepository.findAllByUserId(currentUser.id()).map { it.toResponse() },
        )

    /**
     * Divide um novo aporte pelos maiores desvios das metas, sem vender
     * posições existentes nem ultrapassar o valor recebido.
     */
    override fun allocateContribution(contribution: Money): List<ContributionAllocation> {
        if (!contribution.isPositive()) return emptyList()
        val targets = targetRepository.findAllByUserId(currentUser.id())
        if (targets.isEmpty()) return emptyList()
        val positions = positionRepository.findAllByUserId(currentUser.id())
        positions.forEach { position ->
            require(position.currency == contribution.currency.currencyCode) {
                "A carteira deve estar consolidada na moeda do aporte"
            }
        }
        val currentByClass =
            positions
                .groupBy { it.assetClass }
                .mapValues { (_, values) -> values.fold(BigDecimal.ZERO) { acc, item -> acc + item.currentValue } }
        val currentTotal = currentByClass.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val postContributionTotal = currentTotal + contribution.amount
        val gaps =
            targets.associateWith { target ->
                val desired =
                    postContributionTotal
                        .multiply(target.targetPercentage)
                        .divide(BigDecimal("100"), 8, RoundingMode.HALF_EVEN)
                (desired - currentByClass.getOrDefault(target.assetClass, BigDecimal.ZERO)).max(BigDecimal.ZERO)
            }
        val gapTotal = gaps.values.fold(BigDecimal.ZERO, BigDecimal::add)
        if (gapTotal.compareTo(BigDecimal.ZERO) == 0) return emptyList()
        val allocations =
            gaps
                .filterValues { it > BigDecimal.ZERO }
                .map { (target, gap) ->
                    target.assetClass to
                        contribution.amount
                            .multiply(gap)
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
