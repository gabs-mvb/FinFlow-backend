package com.finflow.portfolio.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.portfolio.application.port.outbound.PortfolioPositionRepository
import com.finflow.portfolio.domain.PortfolioPosition
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.BusinessRuleException
import java.time.Clock
import java.time.OffsetDateTime

class PortfolioService(
    private val currentUser: CurrentUser,
    private val positionRepository: PortfolioPositionRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : PortfolioUseCases {
    override fun replace(request: ReplacePortfolioRequest): PortfolioResponse {
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
        auditService.record("PORTFOLIO_REPLACED", "PORTFOLIO", details = "positions=${request.positions.size}")
        return get()
    }

    override fun get(): PortfolioResponse =
        PortfolioResponse(
            positions = positionRepository.findAllByUserId(currentUser.id()).map { it.toResponse() },
        )

}
