package com.finflow.onboarding.application.port.outbound

enum class OnboardingStep {
    STATUS_READ,
    USER_LOCK_REQUESTED,
    USER_LOCK_ACQUIRED,
    ALREADY_COMPLETED,
    PROFILE_SAVE_STARTED,
    PROFILE_SAVE_FINISHED,
    COMPLETION_SAVE_STARTED,
    COMPLETION_SAVE_FINISHED,
}

fun interface OnboardingEvents {
    fun record(
        step: OnboardingStep,
        userId: Int,
        completed: Boolean?,
    )
}
