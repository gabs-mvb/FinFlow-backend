package com.finflow.onboarding.application.model

data class OnboardingStatus(
    val completed: Boolean,
    /** True only after server completion and verification of the user's own financial context. */
    val readyForDashboard: Boolean = false,
    val status: com.finflow.onboarding.domain.OnboardingProgress = if (completed) com.finflow.onboarding.domain.OnboardingProgress.COMPLETED else com.finflow.onboarding.domain.OnboardingProgress.NOT_STARTED,
    val currentStep: com.finflow.onboarding.domain.OnboardingStage = if (completed) com.finflow.onboarding.domain.OnboardingStage.COMPLETED else com.finflow.onboarding.domain.OnboardingStage.WELCOME,
    val data: com.finflow.onboarding.domain.OnboardingData = com.finflow.onboarding.domain.OnboardingData(),
    val startedAt: java.time.OffsetDateTime? = null,
    val completedAt: java.time.OffsetDateTime? = null,
    val updatedAt: java.time.OffsetDateTime? = null,
)
