package com.finflow

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class HexagonalJourneyTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val json: ObjectMapper,
        private val jdbc: JdbcTemplate,
    ) {
        @Test
        fun `financial journey survives HTTP mapping persistence and transaction boundaries`() {
            val token = registerAndLogin()
            assertFalse(call("GET", "/api/v1/onboarding", token)["completed"].asBoolean())
            call("GET", "/api/v1/plans/latest", token, expected = 204)
            call("POST", "/api/v1/onboarding/complete", token, profile)
            assertTrue(call("GET", "/api/auth/me", token)["onboardingCompleted"].asBoolean())
            // A repeated submission must not overwrite the existing profile.
            call("POST", "/api/v1/onboarding/complete", token, profile.replace("7600", "9999"))
            assertEquals(7600, call("GET", "/api/v1/profile", token)["monthlyIncome"]["amount"].asInt())

            val account = call("POST", "/api/v1/accounts", token, accountBody, 201)
            val id = account["id"].asString()
            call("PATCH", "/api/v1/accounts/$id/balance", token, """{"availableBalance":{"amount":12000.00}}""")
            val batch = """{"accountId":"$id","transactions":[{"externalId":"tx-1","type":"DEBIT","amount":{"amount":100.00},"description":"Supermercado","occurredAt":"2026-09-15T12:00:00Z"}]}"""
            val imported = call("POST", "/api/v1/transactions/imports", token, batch, key = "batch")
            assertEquals(1, imported["imported"].asInt())
            assertTrue(call("POST", "/api/v1/transactions/imports", token, batch, key = "batch")["idempotentReplay"].asBoolean())
            call("POST", "/api/v1/transactions/imports", token, batch.replace("100.00", "200.00"), 409, "batch")

            val obligation =
                call(
                    "POST",
                    "/api/v1/obligations",
                    token,
                    """{"name":"Rent","type":"HOUSING","amount":{"amount":1000.00},"dueDate":"2026-09-25"}""",
                    201,
                )
            val debt =
                call(
                    "POST",
                    "/api/v1/debts",
                    token,
                    """{"name":"Card","type":"CREDIT_CARD","outstandingAmount":{"amount":500.00},"monthlyPayment":{"amount":100.00},"annualEffectiveRate":1.0,"priority":"HIGH_COST"}""",
                    201,
                )
            val goal =
                call(
                    "POST",
                    "/api/v1/goals",
                    token,
                    """{"name":"Trip","targetAmount":{"amount":1000.00},"currentAmount":{"amount":100.00}}""",
                    201,
                )
            assertEquals(
                "ACHIEVED",
                call(
                    "PATCH",
                    "/api/v1/goals/${goal["id"].asString()}/progress",
                    token,
                    """{"currentAmount":{"amount":1000.00}}""",
                )["status"].asString(),
            )

            val consent = """{"provider":"sandbox","externalConsentId":"consent-1","institution":"Bank","scopes":["ACCOUNTS","BALANCES"],"status":"ACTIVE","expiresAt":"${OffsetDateTime.now().plusDays(
                1,
            )}"}"""
            call("PUT", "/api/v1/open-finance/consents", token, consent)
            assertEquals(2, call("GET", "/api/v1/open-finance/consents", token)[0]["scopes"].size())
            val portfolio = """{"positions":[{"assetCode":"CDB","assetName":"Fixed","assetClass":"FIXED_INCOME","currentValue":{"amount":1000.00}}]}"""
            call("PUT", "/api/v1/portfolio", token, portfolio)
            call("PUT", "/api/v1/portfolio", token, portfolio)
            assertEquals(1, call("GET", "/api/v1/portfolio", token)["positions"].size())
            assertFalse(call("GET", "/api/v1/portfolio", token).has("targets"))
            val plan = call("POST", "/api/v1/plans?asOf=2026-09-23", token)
            assertEquals(12000, plan["operatingBalance"]["amount"].asInt())
            assertEquals(12000, plan["totalConsolidatedBalance"]["amount"].asInt())
            assertEquals(plan["id"], call("GET", "/api/v1/plans/latest", token)["id"])
            val action = plan["actions"].first { it["requiresApproval"].asBoolean() }
            val actionPath = "/api/v1/plans/actions/${action["id"].asString()}/approve"
            val approved = call("PATCH", actionPath, token)
            assertEquals("APPROVED", approved["status"].asString())
            assertFalse(approved["executionAvailable"].asBoolean())
            call("PATCH", actionPath, token, expected = 422)
            assertEquals("PAID", call("PATCH", "/api/v1/debts/${debt["id"].asString()}/paid", token)["status"].asString())
            assertEquals("PAID", call("PATCH", "/api/v1/obligations/${obligation["id"].asString()}/paid", token)["status"].asString())
            assertEquals(100, call("GET", "/api/v1/reports/monthly?year=2026&month=9", token)["totalExpenses"]["amount"].asInt())

            val other = registerAndLogin()
            assertEquals(0, call("GET", "/api/v1/accounts", other).size())
            call("PATCH", "/api/v1/accounts/$id/balance", other, """{"availableBalance":{"amount":1.00}}""", 404)
            call("PATCH", actionPath, other, expected = 404)
            val userId = call("GET", "/api/auth/me", token)["id"].asInt()
            assertTrue(jdbc.queryForObject("select count(*) from audit_events where user_id = ?", Int::class.java, userId)!! > 10)
        }

        @Test
        fun `nested HTTP validation and authentication errors preserve status codes`() {
            val token = registerAndLogin()
            call("POST", "/api/v1/accounts", token, accountBody.replace("10000.00", "-1.00"), 400)
            assertEquals(0, call("GET", "/api/v1/accounts", token).size())
            call("POST", "/api/auth/login", body = """{"email":"missing@example.test","password":"incorrect"}""", expected = 401)
        }

        private fun registerAndLogin(): String {
            val email = "hex-${UUID.randomUUID()}@example.test"
            call(
                "POST",
                "/api/auth/register",
                body = """{"name":"Hex Test","email":"$email","password":"strong-test-password"}""",
                expected = 201,
            )
            return call("POST", "/api/auth/login", body = """{"email":"$email","password":"strong-test-password"}""")["token"].asString()
        }

        private fun call(
            method: String,
            url: String,
            token: String? = null,
            body: String? = null,
            expected: Int = 200,
            key: String? = null,
        ): JsonNode {
            val builder = request(HttpMethod.valueOf(method), url)
            if (token != null) builder.header("Authorization", "Bearer $token")
            if (key != null) builder.header("Idempotency-Key", key)
            if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body)
            val response =
                mvc
                    .perform(builder)
                    .andExpect(status().`is`(expected))
                    .andReturn()
                    .response.contentAsString
            return json.readTree(response.ifBlank { "null" })
        }

        private val accountBody = """{"institution":"Bank","externalId":"main","name":"Main","accountType":"CHECKING","purpose":"OPERATING","availableBalance":{"amount":10000.00}}"""
        private val profile = """{"monthlyIncome":{"amount":7600.00},"payDay":5,"essentialMonthlyExpenses":{"amount":3500.00},"variableMonthlyBudget":{"amount":1200.00},"minimumCashBuffer":{"amount":500.00},"emergencyTargetMonths":6,"reserveContributionRate":0.10,"investmentContributionRate":0.10,"riskProfile":"MODERATE","autopilotMode":"COPILOT"}"""
    }
