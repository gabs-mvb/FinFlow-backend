package com.finflow.onboarding.application

import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.AccountPurpose
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.UpdateObligationRequest
import com.finflow.obligation.application.port.inbound.ObligationUseCases
import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.ProgressiveOnboardingUseCases
import com.finflow.onboarding.application.port.outbound.UserOnboardingRepository
import com.finflow.onboarding.domain.*
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.model.MoneyInput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ConflictException
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime

/** All mutations execute under the authenticated user's lock in one transaction. */
class ProgressiveOnboardingService(
    private val currentUser: CurrentUser,
    private val users: UserRepository,
    private val repository: UserOnboardingRepository,
    private val accounts: FinancialAccountUseCases,
    private val accountRepository: FinancialAccountRepository,
    private val profiles: FinancialProfileUseCases,
    private val obligations: ObligationUseCases,
    private val clock: Clock,
) : ProgressiveOnboardingUseCases {
    override fun status(): OnboardingStatus = response(load())

    private fun load(): UserOnboarding = repository.findByUserId(currentUser.id()) ?: UserOnboarding(currentUser.id())

    override fun start(): OnboardingStatus = save(OnboardingStage.WELCOME) { it }

    override fun goal(goals: List<FinancialObjective>): OnboardingStatus = save(OnboardingStage.FINANCIAL_GOAL) {
        require(goals.size == 1) { "Escolha um objetivo principal" }
        it.copy(goals = goals)
    }

    override fun income(incomes: List<IncomeSource>): OnboardingStatus = save(OnboardingStage.INCOME) {
        require(incomes.size == 1) { "Informe a renda principal" }
        incomes.forEach { source ->
            text(source.name, 120)
            money(source.amount)
            require(if (source.schedule == IncomeSchedule.DAY_OF_MONTH) source.payDay in 1..31 else source.payDay == null) { "Dia de recebimento inválido" }
        }
        it.copy(incomes = incomes)
    }

    override fun institutions(institutions: List<String>): OnboardingStatus = save(OnboardingStage.FINANCIAL_INSTITUTIONS) {
        require(institutions.size in 1..30 && institutions.distinct().size == institutions.size) { "Selecione instituições diferentes" }
        institutions.forEach { institution -> text(institution, 120) }
        require(it.accounts.all { account -> account.institution in institutions } && it.cards.all { card -> card.institution in institutions }) { "Mantenha as instituições das contas e cartões cadastrados" }
        it.copy(institutions = institutions)
    }

    override fun accounts(accounts: List<OnboardingAccount>): OnboardingStatus = save(OnboardingStage.ACCOUNTS) { data ->
        require(accounts.size in 1..50 && accounts.distinctBy { it.localId }.size == accounts.size) { "Cadastre ao menos uma conta, sem identificadores repetidos" }
        require(data.accounts.all { old -> accounts.any { it.localId == old.localId } }) { "Contas já salvas devem ser mantidas neste fluxo" }
        val saved = accounts.map { input ->
            text(input.name, 120)
            require(input.institution in data.institutions) { "Selecione uma instituição cadastrada" }
            money(input.balance)
            val existing = data.accounts.find { it.localId == input.localId }
            require(input.accountId == null || input.accountId == existing?.accountId) { "Referência de conta inválida" }
            val externalId = "onboarding-${input.localId}"
            val result = if (existing?.accountId != null) {
                this.accounts.update(existing.accountId, UpdateAccountRequest(input.institution, externalId, input.name, input.type, AccountPurpose.OPERATING, MoneyInput(input.balance)))
            } else {
                this.accounts.create(CreateAccountRequest(input.institution, externalId, input.name, input.type, AccountPurpose.OPERATING, MoneyInput(input.balance)))
            }
            input.copy(accountId = result.id)
        }
        data.copy(accounts = saved)
    }

    override fun cards(cards: List<OnboardingCard>): OnboardingStatus = save(OnboardingStage.CREDIT_CARDS) { data ->
        validateCards(cards, data.institutions)
        data.copy(cards = cards)
    }
    private fun validateCards(cards: List<OnboardingCard>, institutions: List<String>) {
        require(cards.size <= 30 && cards.distinctBy { it.localId }.size == cards.size) { "Cartões inválidos" }
        cards.forEach {
            text(it.name, 120)
            text(it.institution, 120)
            require(it.institution in institutions) { "Instituição do cartão inválida" }
            money(it.limit); money(it.usedLimit)
            require(it.limit > BigDecimal.ZERO && it.usedLimit <= it.limit && it.closingDay in 1..31 && it.dueDay in 1..31) { "Limite ou dias do cartão inválidos" }
        }
    }

    override fun listCards(): List<OnboardingCard> = load().data.cards
    override fun replaceCards(cards: List<OnboardingCard>): List<OnboardingCard> {
        users.lockById(currentUser.id())
        val state = load()
        if (state.status != OnboardingProgress.COMPLETED) throw com.finflow.shared.domain.OnboardingRequiredException()
        validateCards(cards, (state.data.institutions + accountRepository.findAllByUserId(state.userId).map { it.institution }).distinct())
        // An outstanding bill must be settled before removing its card.
        require(state.data.cards.none { old -> old.usedLimit.signum() > 0 && cards.none { it.localId == old.localId } }) { "Quite o limite utilizado antes de remover o cartão" }
        return repository.save(state.copy(data = state.data.copy(cards = cards), updatedAt = OffsetDateTime.now(clock))).data.cards
    }

    override fun profile(profile: InitialFinancialProfile): OnboardingStatus = save(OnboardingStage.FINANCIAL_PROFILE) { data ->
        money(profile.monthlySavings); money(profile.reserveAmount)
        require(profile.reserveStatus != EmergencyReserveStatus.NONE || profile.reserveAmount.signum() == 0) { "Informe zero para ausência de reserva" }
        require(profile.reserveStatus == EmergencyReserveStatus.NONE || profile.reserveAmount.signum() > 0) { "Informe o valor da reserva" }
        require(profile.expenses.size <= 30 && profile.expenses.distinctBy { it.localId }.size == profile.expenses.size) { "Despesas inválidas" }
        val expenses = profile.expenses.map { input ->
            text(input.name, 160); money(input.amount)
            require(input.amount.signum() > 0 && input.dueDay in 1..31) { "Despesa ou vencimento inválido" }
            val old = data.profile?.expenses?.find { it.localId == input.localId }
            require(input.obligationId == null || input.obligationId == old?.obligationId) { "Referência de despesa inválida" }
            val saved = if (old?.obligationId == null) {
                obligations.create(CreateObligationRequest(input.name, ObligationType.OTHER, MoneyInput(input.amount), recurring = true, dueDay = input.dueDay))
            } else {
                obligations.update(old.obligationId, UpdateObligationRequest(input.name, ObligationType.OTHER, MoneyInput(input.amount), null, ObligationStatus.PENDING, recurring = true, dueDay = input.dueDay))
            }
            input.copy(obligationId = saved.id)
        }
        data.profile?.expenses.orEmpty().filter { old -> expenses.none { it.localId == old.localId } }.forEach { old ->
            old.obligationId?.let { id -> obligations.update(id, UpdateObligationRequest(old.name, ObligationType.OTHER, MoneyInput(old.amount), null, ObligationStatus.CANCELLED, recurring = true, dueDay = old.dueDay)) }
        }
        val saved = profile.copy(expenses = expenses)
        profiles.upsert(profileCommand(data.copy(profile = saved)))
        data.copy(profile = saved)
    }

    override fun automation(enabled: Boolean): OnboardingStatus = save(OnboardingStage.AUTOMATION) { it.copy(automationEnabled = enabled) }

    override fun finish(): OnboardingStatus {
        val user = users.lockById(currentUser.id())
        val state = load()
        if (state.status == OnboardingProgress.COMPLETED) return response(state)
        require(state.currentStep == OnboardingStage.SUMMARY && state.data.goals.isNotEmpty() && state.data.incomes.isNotEmpty() && state.data.institutions.isNotEmpty() && state.data.accounts.isNotEmpty() && state.data.profile != null && state.data.automationEnabled != null) { "Conclua todas as etapas obrigatórias" }
        state.data.accounts.forEach { accounts.getRequired(requireNotNull(it.accountId)) }
        profiles.upsert(profileCommand(state.data))
        users.save(user.copy(onboardingCompleted = true))
        val now = OffsetDateTime.now(clock)
        return response(repository.save(state.copy(status = OnboardingProgress.COMPLETED, currentStep = OnboardingStage.COMPLETED, completedAt = now, updatedAt = now)))
    }

    private fun save(step: OnboardingStage, change: (OnboardingData) -> OnboardingData): OnboardingStatus {
        users.lockById(currentUser.id())
        val state = load()
        if (state.status == OnboardingProgress.COMPLETED) throw ConflictException("Onboarding já concluído", "ONBOARDING_COMPLETED")
        require(step.ordinal <= state.currentStep.ordinal) { "Conclua a etapa anterior" }
        val data = change(state.data)
        val next = OnboardingStage.entries[maxOf(state.currentStep.ordinal, step.ordinal + 1)]
        val now = OffsetDateTime.now(clock)
        return response(repository.save(state.copy(status = OnboardingProgress.IN_PROGRESS, currentStep = next, data = data, startedAt = state.startedAt ?: now, updatedAt = now)))
    }

    private fun profileCommand(data: OnboardingData): UpsertFinancialProfileRequest {
        val source = data.incomes.single()
        val profile = requireNotNull(data.profile)
        val totalExpenses = profile.expenses.fold(BigDecimal.ZERO) { sum, it -> sum + it.amount }
        // Protect the user's chosen savings target even before a detailed budget exists.
        return UpsertFinancialProfileRequest(MoneyInput(source.amount), source.payDay ?: 31, MoneyInput(totalExpenses), MoneyInput(BigDecimal.ZERO), MoneyInput(profile.monthlySavings), reserveContributionRate = BigDecimal.ZERO, investmentContributionRate = BigDecimal.ZERO)
    }

    private fun response(state: UserOnboarding): OnboardingStatus {
        val completed = state.status == OnboardingProgress.COMPLETED
        // Completion is durable; removing a financial account later does not restart onboarding.
        return OnboardingStatus(completed, completed, state.status, state.currentStep, state.data, state.startedAt, state.completedAt, state.updatedAt)
    }

    private fun text(value: String, max: Int) = require(value.isNotBlank() && value.length <= max) { "Texto inválido" }
    private fun money(value: BigDecimal, negative: Boolean = false) = require((negative || value.signum() >= 0) && value.stripTrailingZeros().scale() <= 2 && value.precision() - value.scale() <= 17) { "Valor monetário inválido; use até duas casas decimais" }
}
