package com.finflow.onboarding.application

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.onboarding.application.port.outbound.OnboardingEvents
import com.finflow.onboarding.application.port.outbound.OnboardingStep
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.port.outbound.CurrentUser

class OnboardingService(
    private val currentUser: CurrentUser,
    private val profiles: FinancialProfileUseCases,
    private val users: UserRepository,
    private val events: OnboardingEvents,
) : OnboardingUseCases {
    override fun status(): OnboardingStatus {
        val user = currentUser.user()
        events.record(OnboardingStep.STATUS_READ, user.id, user.onboardingCompleted)
        return OnboardingStatus(user.onboardingCompleted)
    }

    override fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus {
        val userId = currentUser.id()
        events.record(OnboardingStep.USER_LOCK_REQUESTED, userId, null)
        val user = users.lockById(userId)
        events.record(OnboardingStep.USER_LOCK_ACQUIRED, user.id, user.onboardingCompleted)
        if (!user.onboardingCompleted) {
            events.record(OnboardingStep.PROFILE_SAVE_STARTED, user.id, false)
            profiles.upsert(request)
            events.record(OnboardingStep.PROFILE_SAVE_FINISHED, user.id, false)
            events.record(OnboardingStep.COMPLETION_SAVE_STARTED, user.id, false)
            users.save(user.copy(onboardingCompleted = true))
            events.record(OnboardingStep.COMPLETION_SAVE_FINISHED, user.id, true)
        } else {
            events.record(OnboardingStep.ALREADY_COMPLETED, user.id, true)
        }
        return OnboardingStatus(true)
    }
}
