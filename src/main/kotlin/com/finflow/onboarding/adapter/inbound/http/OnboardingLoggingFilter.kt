package com.finflow.onboarding.adapter.inbound.http

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class OnboardingLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return path != "/api/v1/onboarding" && !path.startsWith("/api/v1/onboarding/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val previous = MDC.get("onboardingRequestId")
        val requestId = UUID.randomUUID().toString()
        val started = System.nanoTime()
        MDC.put("onboardingRequestId", requestId)
        response.setHeader("X-Request-ID", requestId)
        // Do not log the body, query, authorization header or arbitrary client-provided paths.
        log.info("event=onboarding.http.started requestId={} method={}", requestId, request.method)
        var escapedFailure = false
        try {
            filterChain.doFilter(request, response)
        } catch (exception: Exception) {
            escapedFailure = true
            log.error("event=onboarding.http.failed requestId={} exceptionType={}", requestId, exception.javaClass.simpleName)
            throw exception
        } finally {
            val status = if (escapedFailure) 500 else response.status
            val message = "event=onboarding.http.finished requestId={} status={} code={} durationMs={}"
            val args =
                arrayOf(
                    requestId,
                    status,
                    request.getAttribute("onboardingErrorCode") ?: "none",
                    (System.nanoTime() - started) / 1_000_000,
                )
            when {
                status >= 500 -> log.error(message, *args)
                status >= 400 -> log.warn(message, *args)
                else -> log.info(message, *args)
            }
            if (previous == null) MDC.remove("onboardingRequestId") else MDC.put("onboardingRequestId", previous)
        }
    }
}
