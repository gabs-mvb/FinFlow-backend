package com.finflow

import com.finflow.planning.adapter.outbound.ai.OpenAiPlanAdvisor
import com.finflow.planning.application.model.PlanningEvidence
import com.finflow.planning.application.model.PlanningProfile
import com.finflow.planning.domain.PlanContent
import com.finflow.profile.domain.AutopilotMode
import com.finflow.profile.domain.RiskProfile
import com.finflow.shared.domain.AiPlanningException
import com.sun.net.httpserver.HttpServer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenAiPlanAdvisorTest {
    private val json = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val date = LocalDate.parse("2026-09-24")
    private val zero = BigDecimal.ZERO
    private val content =
        PlanContent(
            date,
            date.plusDays(10),
            "Plano",
            "Análise personalizada",
            zero,
            zero,
            zero,
            zero,
            zero,
            zero,
            zero,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )
    private val evidence =
        PlanningEvidence(
            date,
            date.minusDays(89),
            date,
            0,
            0,
            PlanningProfile("BRL", zero, 5, zero, zero, zero, 6, zero, zero, RiskProfile.CONSERVATIVE, AutopilotMode.OBSERVER),
            zero,
            zero,
            zero,
            false,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            content,
        )

    @Test
    fun `Responses request uses structured output non storage and server credentials`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var requestBody = ""
        var authorization = ""
        server.createContext("/responses") { exchange ->
            requestBody = exchange.requestBody.bufferedReader().readText()
            authorization = exchange.requestHeaders.getFirst("Authorization")
            val body =
                json.writeValueAsBytes(
                    mapOf(
                        "status" to "completed",
                        "output" to
                            listOf(
                                mapOf(
                                    "type" to "message",
                                    "content" to
                                        listOf(
                                            mapOf("type" to "output_text", "text" to json.writeValueAsString(content)),
                                        ),
                                ),
                            ),
                    ),
                )
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val result = adapter(server).suggest(evidence, "Reserva primeiro")
            assertEquals(content, result.content)
            val body = json.readTree(requestBody)
            assertEquals("Bearer test-key", authorization)
            assertEquals("test-model", body["model"].asString())
            assertFalse(body["store"].asBoolean())
            assertTrue(body["text"]["format"]["strict"].asBoolean())
            assertEquals("json_schema", body["text"]["format"]["type"].asString())
            assertFalse(body["text"]["format"]["schema"]["additionalProperties"].asBoolean())
            assertTrue(body["input"].asString().contains("Reserva primeiro"))
            assertFalse(body["input"].asString().contains("test-key"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `provider errors refusal incomplete and malformed output fail without leaking response`() {
        for ((status, body) in listOf(
            429 to "SECRET provider body",
            401 to "SECRET key",
            200 to """{"status":"incomplete","output":[]}""",
            200 to """{"status":"completed","output":[{"content":[{"type":"refusal","refusal":"SECRET"}]}]}""",
            200 to """{"status":"completed","output":[{"content":[{"type":"output_text","text":"{}"}]}]}""",
            200 to "invalid JSON",
        )) {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/responses") { exchange ->
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            try {
                val error = assertFailsWith<AiPlanningException> { adapter(server).suggest(evidence, "") }
                assertFalse(error.message.contains("SECRET"))
            } finally {
                server.stop(0)
            }
        }
        val error = assertFailsWith<AiPlanningException> { OpenAiPlanAdvisor(json, "", "", false).suggest(evidence, "") }
        assertEquals("AI_NOT_CONFIGURED", error.code)
    }

    @Test
    fun `request times out`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/responses") { exchange ->
            Thread.sleep(200)
            exchange.close()
        }
        server.start()
        try {
            assertFailsWith<AiPlanningException> { adapter(server, Duration.ofMillis(30)).suggest(evidence, "") }
        } finally {
            server.stop(0)
        }
    }

    private fun adapter(
        server: HttpServer,
        timeout: Duration = Duration.ofSeconds(2),
    ) = OpenAiPlanAdvisor(json, "test-key", "test-model", true, timeout, URI.create("http://127.0.0.1:${server.address.port}/responses"))
}
