package com.finflow.profile.application

import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.profile.application.model.FinancialProfileResponse
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.profile.domain.FinancialProfile
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.ResourceNotFoundException
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency

class FinancialProfileService(
    private val currentUser: CurrentUser,
    private val repository: FinancialProfileRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : FinancialProfileUseCases {
    override fun upsert(request: UpsertFinancialProfileRequest): FinancialProfileResponse {
        val monthlyIncome = request.monthlyIncome.toMoney()
        val essentialExpenses = request.essentialMonthlyExpenses.toMoney()
        val variableBudget = request.variableMonthlyBudget.toMoney()
        val minimumCashBuffer = request.minimumCashBuffer.toMoney()
        val currency = monthlyIncome.currency
        requireCurrenciesMatch(currency, essentialExpenses, variableBudget, minimumCashBuffer)
        val now = OffsetDateTime.now(clock)
        val profile =
            repository.findByUserId(currentUser.id())
                ?: FinancialProfile(userId = currentUser.id(), createdAt = now)
        val updated =
            profile.copy(
                currency = currency.currencyCode,
                monthlyIncome = monthlyIncome.amount,
                payDay = request.payDay,
                essentialMonthlyExpenses = essentialExpenses.amount,
                variableMonthlyBudget = variableBudget.amount,
                minimumCashBuffer = minimumCashBuffer.amount,
                emergencyTargetMonths = request.emergencyTargetMonths,
                reserveContributionRate = request.reserveContributionRate,
                investmentContributionRate = request.investmentContributionRate,
                riskProfile = request.riskProfile,
                autopilotMode = request.autopilotMode,
                updatedAt = now,
            )
        val saved = repository.save(updated)
        auditService.record("PROFILE_UPSERTED", "FINANCIAL_PROFILE", saved.id)
        return saved.toResponse()
    }

    override fun getRequired(): FinancialProfile =
        repository.findByUserId(currentUser.id())
            ?: throw ResourceNotFoundException("Configure o perfil financeiro antes de calcular o plano")

    override fun get(): FinancialProfileResponse = getRequired().toResponse()

    private fun requireCurrenciesMatch(
        currency: Currency,
        vararg amounts: Money,
    ) {
        require(amounts.all { it.currency == currency }) {
            "Todos os valores do perfil devem usar a mesma moeda"
        }
    }
}

private fun FinancialProfile.toResponse(): FinancialProfileResponse {
    val parsedCurrency = Currency.getInstance(currency)
    return FinancialProfileResponse(
        monthlyIncome = Money(monthlyIncome, parsedCurrency).toOutput(),
        payDay = payDay,
        essentialMonthlyExpenses = Money(essentialMonthlyExpenses, parsedCurrency).toOutput(),
        variableMonthlyBudget = Money(variableMonthlyBudget, parsedCurrency).toOutput(),
        minimumCashBuffer = Money(minimumCashBuffer, parsedCurrency).toOutput(),
        emergencyTargetMonths = emergencyTargetMonths,
        reserveContributionRate = reserveContributionRate,
        investmentContributionRate = investmentContributionRate,
        riskProfile = riskProfile,
        autopilotMode = autopilotMode,
        updatedAt = updatedAt,
    )
}
