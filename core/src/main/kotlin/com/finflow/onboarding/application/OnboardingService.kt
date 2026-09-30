package com.finflow.onboarding.application

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.onboarding.application.port.outbound.OnboardingEvents
import com.finflow.onboarding.application.port.outbound.OnboardingStep
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.shared.application.port.outbound.CurrentUser

class OnboardingService(
    private val currentUser: CurrentUser,
    private val profileUseCases: FinancialProfileUseCases,
    private val profiles: FinancialProfileRepository,
    private val accounts: FinancialAccountRepository,
    private val users: UserRepository,
    private val events: OnboardingEvents,
) : OnboardingUseCases {
    override fun status(): OnboardingStatus {
        val user = currentUser.user()
        events.record(OnboardingStep.STATUS_READ, user.id, user.onboardingCompleted)
        return statusFor(user.id, user.onboardingCompleted)
    }

    override fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus {
        val userId = currentUser.id()
        events.record(OnboardingStep.USER_LOCK_REQUESTED, userId, null)
        val user = users.lockById(userId)
        events.record(OnboardingStep.USER_LOCK_ACQUIRED, user.id, user.onboardingCompleted)
        if (!user.onboardingCompleted) {
            events.record(OnboardingStep.PROFILE_SAVE_STARTED, user.id, false)
            profileUseCases.upsert(request)
            events.record(OnboardingStep.PROFILE_SAVE_FINISHED, user.id, false)
            events.record(OnboardingStep.COMPLETION_SAVE_STARTED, user.id, false)
            users.save(user.copy(onboardingCompleted = true))
            events.record(OnboardingStep.COMPLETION_SAVE_FINISHED, user.id, true)
        } else {
            events.record(OnboardingStep.ALREADY_COMPLETED, user.id, true)
        }
        return statusFor(user.id, true)
    }

    private fun statusFor(
        userId: Int,
        legacyCompleted: Boolean,
    ): OnboardingStatus =
        OnboardingStatus(
            completed = legacyCompleted,
            readyForDashboard =
                profiles.findByUserId(userId) != null && accounts.findAllByUserId(userId).isNotEmpty(),
        )
}
