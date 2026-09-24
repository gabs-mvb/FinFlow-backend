package com.finflow.onboarding.adapter.outbound.logging

import com.finflow.onboarding.application.port.outbound.OnboardingEvents
import com.finflow.onboarding.application.port.outbound.OnboardingStep
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class Slf4jOnboardingEvents : OnboardingEvents {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun record(
        step: OnboardingStep,
        userId: Int,
        completed: Boolean?,
    ) {
        logger.info(
            "event=onboarding.step requestId={} step={} userId={} completed={}",
            MDC.get("onboardingRequestId") ?: "none",
            step,
            userId,
            completed,
        )
    }
}
