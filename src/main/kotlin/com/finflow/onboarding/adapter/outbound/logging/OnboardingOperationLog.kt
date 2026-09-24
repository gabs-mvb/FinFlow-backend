package com.finflow.onboarding.adapter.outbound.logging

import org.slf4j.LoggerFactory
import org.slf4j.MDC

object OnboardingOperationLog {
    private val logger = LoggerFactory.getLogger(OnboardingOperationLog::class.java)

    fun <T> observe(
        operation: String,
        block: () -> T,
    ): T {
        val started = System.nanoTime()
        val requestId = MDC.get("onboardingRequestId") ?: "none"
        logger.info("event=onboarding.operation.started requestId={} operation={}", requestId, operation)
        try {
            val result = block()
            // The block includes TransactionTemplate.execute: success means commit has returned.
            logger.info(
                "event=onboarding.operation.committed requestId={} operation={} durationMs={}",
                requestId,
                operation,
                (System.nanoTime() - started) / 1_000_000,
            )
            return result
        } catch (exception: Exception) {
            logger.error(
                "event=onboarding.operation.failed requestId={} operation={} exceptionType={} durationMs={}",
                requestId,
                operation,
                exception.javaClass.simpleName,
                (System.nanoTime() - started) / 1_000_000,
            )
            throw exception
        }
    }
}
