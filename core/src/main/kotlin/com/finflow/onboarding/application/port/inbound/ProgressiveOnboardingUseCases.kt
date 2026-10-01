package com.finflow.onboarding.application.port.inbound

import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.domain.*

interface ProgressiveOnboardingUseCases {
    fun status(): OnboardingStatus
    fun start(): OnboardingStatus
    fun goal(goals: List<FinancialObjective>): OnboardingStatus
    fun income(incomes: List<IncomeSource>): OnboardingStatus
    fun institutions(institutions: List<String>): OnboardingStatus
    fun accounts(accounts: List<OnboardingAccount>): OnboardingStatus
    fun cards(cards: List<OnboardingCard>): OnboardingStatus
    fun profile(profile: InitialFinancialProfile): OnboardingStatus
    fun automation(enabled: Boolean): OnboardingStatus
    fun finish(): OnboardingStatus
    fun listCards(): List<OnboardingCard>
    fun replaceCards(cards: List<OnboardingCard>): List<OnboardingCard>
}
