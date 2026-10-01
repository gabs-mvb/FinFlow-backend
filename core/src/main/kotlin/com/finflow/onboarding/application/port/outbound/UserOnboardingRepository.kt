package com.finflow.onboarding.application.port.outbound

import com.finflow.onboarding.domain.UserOnboarding

interface UserOnboardingRepository {
    fun findByUserId(userId: Int): UserOnboarding?
    fun save(value: UserOnboarding): UserOnboarding
}
