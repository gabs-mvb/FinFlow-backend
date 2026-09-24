package com.finflow

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.finflow.onboarding.adapter.inbound.http.OnboardingLoggingFilter
import com.finflow.onboarding.adapter.outbound.logging.OnboardingOperationLog
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingLoggingTest {
    @Test
    fun `rejected requests keep correlation without exposing input and restore MDC`() =
        capture { events ->
            MDC.put("onboardingRequestId", "previous")
            try {
                val request = MockHttpServletRequest("POST", "/api/v1/onboarding/complete")
                request.addHeader("Authorization", "Bearer PRIVATE_TOKEN")
                request.queryString = "email=PRIVATE_EMAIL"
                request.setContent("PRIVATE_FINANCIAL_DATA".toByteArray())
                val response = MockHttpServletResponse()
                OnboardingLoggingFilter().doFilter(request, response) { _, _ ->
                    assertEquals(response.getHeader("X-Request-ID"), MDC.get("onboardingRequestId"))
                    request.setAttribute("onboardingErrorCode", "VALIDATION_ERROR")
                    response.status = 400
                }
                val requestId = assertNotNull(response.getHeader("X-Request-ID"))
                assertEquals("previous", MDC.get("onboardingRequestId"))
                assertTrue(events.any { it.formattedMessage.contains("requestId=$requestId status=400 code=VALIDATION_ERROR") })
                assertFalse(events.any { it.formattedMessage.contains("PRIVATE") })
            } finally {
                MDC.remove("onboardingRequestId")
            }
        }

    @Test
    fun `failed transaction never logs committed and request context is cleaned`() =
        capture { events ->
            val request = MockHttpServletRequest("POST", "/api/v1/onboarding/complete")
            val response = MockHttpServletResponse()
            assertFailsWith<IllegalStateException> {
                OnboardingLoggingFilter().doFilter(request, response) { _, _ ->
                    OnboardingOperationLog.observe("complete") { throw IllegalStateException("PRIVATE_SQL_DATA") }
                }
            }
            assertNull(MDC.get("onboardingRequestId"))
            assertTrue(events.any { it.formattedMessage.contains("onboarding.operation.failed") })
            assertTrue(events.any { it.formattedMessage.contains("status=500") })
            assertFalse(events.any { it.formattedMessage.contains("committed") || it.formattedMessage.contains("PRIVATE") })
            assertTrue(events.all { it.throwableProxy == null })
        }

    private fun capture(block: (List<ILoggingEvent>) -> Unit) {
        val logger = LoggerFactory.getLogger("com.finflow.onboarding") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            block(appender.list)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
