package com.finflow.onboarding.adapter.inbound.http

import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PutMapping
import com.finflow.onboarding.application.port.inbound.ProgressiveOnboardingUseCases
import com.finflow.onboarding.domain.*

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController(
    private val service: OnboardingUseCases,
    private val progressive: ProgressiveOnboardingUseCases,
) {
    @GetMapping
    fun status(): OnboardingStatus = service.status()

    @PostMapping("/complete")
    fun complete(): OnboardingStatus = progressive.finish()

    @PostMapping("/start") fun start() = progressive.start()
    @PutMapping("/goal") fun goal(@RequestBody body: GoalStepBody) = progressive.goal(body.goals)
    @PutMapping("/income") fun income(@RequestBody body: IncomeStepBody) = progressive.income(body.incomes)
    @PutMapping("/institutions") fun institutions(@RequestBody body: InstitutionsStepBody) = progressive.institutions(body.institutions)
    @PutMapping("/accounts") fun accounts(@RequestBody body: AccountsStepBody) = progressive.accounts(body.accounts)
    @PutMapping("/cards") fun cards(@RequestBody body: CardsStepBody) = progressive.cards(body.cards)
    @PutMapping("/profile") fun profile(@RequestBody body: InitialFinancialProfile) = progressive.profile(body)
    @PutMapping("/automation") fun automation(@RequestBody body: AutomationStepBody) = progressive.automation(body.enabled)
}

data class GoalStepBody(val goals: List<FinancialObjective>)
data class IncomeStepBody(val incomes: List<IncomeSource>)
data class InstitutionsStepBody(val institutions: List<String>)
data class AccountsStepBody(val accounts: List<OnboardingAccount>)
data class CardsStepBody(val cards: List<OnboardingCard>)
data class AutomationStepBody(val enabled: Boolean)
