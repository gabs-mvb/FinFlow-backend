package com.finflow.onboarding.domain

import com.finflow.account.domain.AccountType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class OnboardingStage { WELCOME, FINANCIAL_GOAL, INCOME, FINANCIAL_INSTITUTIONS, ACCOUNTS, CREDIT_CARDS, FINANCIAL_PROFILE, AUTOMATION, SUMMARY, COMPLETED }
enum class OnboardingProgress { NOT_STARTED, IN_PROGRESS, COMPLETED }
enum class FinancialObjective { ORGANIZE, PAY_DEBTS, SAVE_MORE, EMERGENCY_RESERVE, CONTROL_SPENDING, SAVE_FOR_GOAL, UNDERSTAND_SPENDING }
enum class IncomeSchedule { DAY_OF_MONTH, LAST_BUSINESS_DAY, VARIABLE }
enum class EmergencyReserveStatus { NONE, PARTIAL, COMPLETE }

data class IncomeSource(val name: String = "Principal", val amount: BigDecimal, val schedule: IncomeSchedule, val payDay: Int? = null)
data class OnboardingAccount(val localId: UUID, val institution: String, val name: String, val type: AccountType, val balance: BigDecimal, val accountId: UUID? = null)
data class OnboardingCard(val localId: UUID, val institution: String, val name: String, val limit: BigDecimal, val closingDay: Int, val dueDay: Int, val usedLimit: BigDecimal = BigDecimal.ZERO)
data class RecurringExpense(val localId: UUID, val name: String, val amount: BigDecimal, val dueDay: Int, val obligationId: UUID? = null)
data class InitialFinancialProfile(val monthlySavings: BigDecimal, val reserveStatus: EmergencyReserveStatus, val reserveAmount: BigDecimal, val expenses: List<RecurringExpense> = emptyList())
data class OnboardingData(
    val goals: List<FinancialObjective> = emptyList(),
    val incomes: List<IncomeSource> = emptyList(),
    val institutions: List<String> = emptyList(),
    val accounts: List<OnboardingAccount> = emptyList(),
    val cards: List<OnboardingCard> = emptyList(),
    val profile: InitialFinancialProfile? = null,
    val automationEnabled: Boolean? = null,
)
data class UserOnboarding(
    val userId: Int,
    val status: OnboardingProgress = OnboardingProgress.NOT_STARTED,
    val currentStep: OnboardingStage = OnboardingStage.WELCOME,
    val data: OnboardingData = OnboardingData(),
    val startedAt: OffsetDateTime? = null,
    val completedAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
