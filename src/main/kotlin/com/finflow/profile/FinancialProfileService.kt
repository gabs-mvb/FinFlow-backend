package com.finflow.profile

import com.finflow.audit.AuditService
import com.finflow.shared.api.ResourceNotFoundException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyInput
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency

data class UpsertFinancialProfileRequest(
    @field:Valid
    val monthlyIncome: MoneyInput,
    @field:Min(1)
    @field:Max(28)
    val payDay: Int,
    @field:Valid
    val essentialMonthlyExpenses: MoneyInput,
    @field:Valid
    val variableMonthlyBudget: MoneyInput,
    @field:Valid
    val minimumCashBuffer: MoneyInput,
    @field:Min(1)
    @field:Max(24)
    val emergencyTargetMonths: Int = 6,
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val reserveContributionRate: BigDecimal = BigDecimal("0.10"),
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val investmentContributionRate: BigDecimal = BigDecimal("0.10"),
    val riskProfile: RiskProfile = RiskProfile.CONSERVATIVE,
    val autopilotMode: AutopilotMode = AutopilotMode.OBSERVER,
)

data class FinancialProfileResponse(
    val monthlyIncome: MoneyOutput,
    val payDay: Int,
    val essentialMonthlyExpenses: MoneyOutput,
    val variableMonthlyBudget: MoneyOutput,
    val minimumCashBuffer: MoneyOutput,
    val emergencyTargetMonths: Int,
    val reserveContributionRate: BigDecimal,
    val investmentContributionRate: BigDecimal,
    val riskProfile: RiskProfile,
    val autopilotMode: AutopilotMode,
    val updatedAt: OffsetDateTime,
)

@Service
@Validated
class FinancialProfileService(
    private val repository: FinancialProfileRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun upsert(request: UpsertFinancialProfileRequest): FinancialProfileResponse {
        val monthlyIncome = request.monthlyIncome.toMoney()
        val essentialExpenses = request.essentialMonthlyExpenses.toMoney()
        val variableBudget = request.variableMonthlyBudget.toMoney()
        val minimumCashBuffer = request.minimumCashBuffer.toMoney()
        val currency = monthlyIncome.currency
        requireCurrenciesMatch(currency, essentialExpenses, variableBudget, minimumCashBuffer)

        val now = OffsetDateTime.now(clock)
        val profile = repository.findFirstByOrderByCreatedAtAsc()
            ?: FinancialProfileEntity(createdAt = now)
        profile.apply {
            this.currency = currency.currencyCode
            this.monthlyIncome = monthlyIncome.amount
            this.payDay = request.payDay
            this.essentialMonthlyExpenses = essentialExpenses.amount
            this.variableMonthlyBudget = variableBudget.amount
            this.minimumCashBuffer = minimumCashBuffer.amount
            this.emergencyTargetMonths = request.emergencyTargetMonths
            this.reserveContributionRate = request.reserveContributionRate
            this.investmentContributionRate = request.investmentContributionRate
            this.riskProfile = request.riskProfile
            this.autopilotMode = request.autopilotMode
            this.updatedAt = now
        }
        val saved = repository.save(profile)
        auditService.record("PROFILE_UPSERTED", "FINANCIAL_PROFILE", saved.id)
        return saved.toResponse()
    }

    @Transactional
    fun getRequired(): FinancialProfileEntity = repository.findFirstByOrderByCreatedAtAsc()
        ?: throw ResourceNotFoundException("Configure o perfil financeiro antes de calcular o plano")

    @Transactional
    fun get(): FinancialProfileResponse = getRequired().toResponse()

    private fun requireCurrenciesMatch(currency: Currency, vararg amounts: Money) {
        require(amounts.all { it.currency == currency }) {
            "Todos os valores do perfil devem usar a mesma moeda"
        }
    }
}

private fun FinancialProfileEntity.toResponse(): FinancialProfileResponse {
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

