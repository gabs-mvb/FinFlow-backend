package com.finflow.financialevent

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class FinancialEventControllerTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val json: ObjectMapper,
    ) {
        @Test
        fun `batch maps parsed events to canonical transactions and deduplicates fingerprints`() {
            val token = registerAndLogin()
            val accountId = createAccount(token)
            val body = eventBatch(accountId)

            val first = postEvents(token, body, "android-batch-1")
            assertEquals(2, first["imported"].asInt())
            assertEquals(0, first["duplicates"].asInt())
            assertFalse(first["idempotentReplay"].asBoolean())

            val transactions =
                call(
                    "GET",
                    "/api/v1/transactions?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z",
                    token,
                )
            assertEquals(2, transactions.size())
            val debit = transactions.first { it["externalId"].asString() == "financial-event:nubank-debit-1" }
            assertEquals("DEBIT", debit["type"].asString())
            assertEquals("TRANSPORT", debit["category"].asString())
            assertEquals("Uber", debit["merchant"].asString())
            val credit = transactions.first { it["externalId"].asString() == "financial-event:nubank-pix-1" }
            assertEquals("CREDIT", credit["type"].asString())
            assertEquals(1417.60, call("GET", "/api/v1/accounts", token)[0]["availableBalance"]["amount"].asDouble())

            val replay = postEvents(token, body, "android-batch-1")
            assertEquals(2, replay["imported"].asInt())
            assertTrue(replay["idempotentReplay"].asBoolean())

            val duplicateFingerprint = postEvents(token, body, "android-batch-2")
            assertEquals(0, duplicateFingerprint["imported"].asInt())
            assertEquals(2, duplicateFingerprint["duplicates"].asInt())
            assertFalse(duplicateFingerprint["idempotentReplay"].asBoolean())
        }

        @Test
        fun `event batches are isolated by the authenticated account owner including idempotency keys`() {
            val ownerToken = registerAndLogin()
            val ownerAccountId = createAccount(ownerToken)
            val otherToken = registerAndLogin()
            val otherAccountId = createAccount(otherToken)

            val owner = postEvents(ownerToken, eventBatch(ownerAccountId), "same-mobile-key")
            val other = postEvents(otherToken, eventBatch(otherAccountId), "same-mobile-key")
            assertEquals(2, owner["imported"].asInt())
            assertEquals(2, other["imported"].asInt())
            assertFalse(other["idempotentReplay"].asBoolean())

            val denied = call("POST", "/api/v1/financial-events/batch", otherToken, eventBatch(ownerAccountId), 404, "other-key")
            assertEquals("RESOURCE_NOT_FOUND", denied["code"].asString())
            assertEquals(1, call("GET", "/api/v1/accounts", otherToken).size())
            assertEquals(1, call("GET", "/api/v1/accounts", ownerToken).size())
        }

        @Test
        fun `dashboard readiness requires a profile and an account owned by the same user`() {
            val token = registerAndLogin()
            val otherToken = registerAndLogin()

            assertFalse(call("GET", "/api/v1/onboarding", token)["readyForDashboard"].asBoolean())
            call("POST", "/api/v1/onboarding/complete", token, financialProfile())
            assertFalse(call("GET", "/api/v1/onboarding", token)["readyForDashboard"].asBoolean())

            createAccount(otherToken)
            assertFalse(call("GET", "/api/v1/onboarding", token)["readyForDashboard"].asBoolean())

            createAccount(token)
            assertTrue(call("GET", "/api/v1/onboarding", token)["readyForDashboard"].asBoolean())
        }

        @Test
        fun `new events update the tracked balance and refresh an existing rule plan`() {
            val token = registerAndLogin()
            call("POST", "/api/v1/onboarding/complete", token, financialProfile())
            val accountId = createAccount(token)
            call("POST", "/api/v1/plans", token)

            postEvents(token, eventBatch(accountId), "plan-refresh")

            val plan = call("GET", "/api/v1/plans/latest", token)
            assertEquals(1417.60, plan["operatingBalance"]["amount"].asDouble())
            assertTrue(plan["dailySpendingLimit"]["amount"].asDouble() >= 0)
        }

        @Test
        fun `unknown events are rejected before they can affect the financial record`() {
            val token = registerAndLogin()
            val accountId = createAccount(token)
            val invalid = eventBatch(accountId).replace("DEBIT_PURCHASE", "UNKNOWN")

            val response = postEvents(token, invalid, "unknown-event", 400)
            assertEquals("INVALID_ARGUMENT", response["code"].asString())
            assertEquals(
                0,
                call(
                    "GET",
                    "/api/v1/transactions?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z",
                    token,
                ).size(),
            )
        }

        private fun registerAndLogin(): String {
            val email = "financial-event-${UUID.randomUUID()}@example.test"
            call(
                "POST",
                "/api/auth/register",
                body = """{"name":"Financial Event Test","email":"$email","password":"strong-test-password"}""",
                expected = 201,
            )
            return call(
                "POST",
                "/api/auth/login",
                body = """{"email":"$email","password":"strong-test-password"}""",
            )["token"].asString()
        }

        private fun createAccount(token: String): String =
            call(
                "POST",
                "/api/v1/accounts",
                token,
                """{"institution":"Nubank","externalId":"${UUID.randomUUID()}","name":"Conta Nubank","accountType":"CHECKING","purpose":"OPERATING","availableBalance":{"amount":1000.00,"currency":"BRL"}}""",
                201,
            )["id"].asString()

        private fun eventBatch(accountId: String): String =
            """
            {
              "accountId":"$accountId",
              "events":[
                {
                  "fingerprint":"nubank-debit-1",
                  "type":"DEBIT_PURCHASE",
                  "amount":32.40,
                  "currency":"BRL",
                  "description":"Compra aprovada Uber",
                  "merchant":"Uber",
                  "occurredAt":"2026-09-25T12:00:00Z"
                },
                {
                  "fingerprint":"nubank-pix-1",
                  "type":"PIX_RECEIVED",
                  "amount":450.00,
                  "currency":"BRL",
                  "description":"Pix recebido",
                  "occurredAt":"2026-09-25T13:00:00Z"
                }
              ]
            }
            """.trimIndent()

        private fun financialProfile(): String =
            """
            {
              "monthlyIncome":{"amount":6000.00,"currency":"BRL"},
              "payDay":5,
              "essentialMonthlyExpenses":{"amount":2500.00,"currency":"BRL"},
              "variableMonthlyBudget":{"amount":1000.00,"currency":"BRL"},
              "minimumCashBuffer":{"amount":300.00,"currency":"BRL"},
              "emergencyTargetMonths":6,
              "reserveContributionRate":0.10,
              "investmentContributionRate":0.10,
              "riskProfile":"CONSERVATIVE",
              "autopilotMode":"OBSERVER"
            }
            """.trimIndent()

        private fun postEvents(
            token: String,
            body: String,
            idempotencyKey: String,
            expected: Int = 200,
        ): JsonNode = call("POST", "/api/v1/financial-events/batch", token, body, expected, idempotencyKey)

        private fun call(
            method: String,
            url: String,
            token: String? = null,
            body: String? = null,
            expected: Int = 200,
            idempotencyKey: String? = null,
        ): JsonNode {
            val builder = request(HttpMethod.valueOf(method), url)
            if (token != null) builder.header("Authorization", "Bearer $token")
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
            if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body)
            val response = mvc.perform(builder).andExpect(status().`is`(expected)).andReturn().response.contentAsString
            return json.readTree(response.ifBlank { "null" })
        }
    }
