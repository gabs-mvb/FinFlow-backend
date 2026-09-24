package com.finflow.onboarding.application

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.shared.application.port.outbound.CurrentUser

class OnboardingService(
    private val currentUser: CurrentUser,
    private val profiles: FinancialProfileUseCases,
    private val users: UserRepository,
) : OnboardingUseCases {
    override fun status(): OnboardingStatus = OnboardingStatus(currentUser.user().onboardingCompleted)

    override fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus {
        val user = users.lockById(currentUser.id())
        if (!user.onboardingCompleted) {
            profiles.upsert(request)
            users.save(user.copy(onboardingCompleted = true))
        }
        return OnboardingStatus(true)
    }
}
