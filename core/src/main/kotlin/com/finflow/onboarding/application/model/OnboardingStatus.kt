package com.finflow.onboarding.application.model

data class OnboardingStatus(
    val completed: Boolean,
    /**
     * Authoritative prerequisite for clients that need a usable financial
     * starting point: the authenticated user owns a saved profile and at least
     * one financial account. `completed` remains the legacy profile flow flag.
     */
    val readyForDashboard: Boolean = false,
)
