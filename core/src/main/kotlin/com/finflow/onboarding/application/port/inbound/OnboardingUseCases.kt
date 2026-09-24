package com.finflow.onboarding.application.port.inbound

import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.profile.application.model.UpsertFinancialProfileRequest

interface OnboardingUseCases {
    fun status(): OnboardingStatus

    fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus
}
