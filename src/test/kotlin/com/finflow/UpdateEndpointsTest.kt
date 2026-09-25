package com.finflow

import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.FinancialAccount
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.Obligation
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.domain.FinancialPlan
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.TestExecutionEvent
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails(value = "updates@finflow.test", setupBefore = TestExecutionEvent.TEST_EXECUTION)
class UpdateEndpointsTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val json: ObjectMapper,
        private val jdbc: JdbcTemplate,
        private val users: UserRepository,
        private val accounts: FinancialAccountRepository,
        private val obligations: ObligationRepository,
        private val plans: FinancialPlanRepository,
    ) {
        private var userId = 0

        @BeforeEach
        fun arrange() {
            userId =
                users
                    .findByEmail("updates@finflow.test")
                    .orElseGet {
                        users.save(User(name = "Updates", email = "updates@finflow.test", password = "unused"))
                    }.id
        }

        @Test
        fun `account PUT replaces editable data preserves identity and validates conflicts`() {
            val external = UUID.randomUUID().toString()
            val body = accountBody(external)
            val created = call("POST", "/api/v1/accounts", body, 201)
            val id = UUID.fromString(created["id"].asString())
            val before = accounts.findByIdAndUserId(id, userId)!!
            val replacement =
                body
                    .replace("Main", " Updated ")
                    .replace("CHECKING", "SAVINGS")
                    .replace("OPERATING", "GOAL")
                    .replace("10000.00", "500.00")
            val updated = call("PUT", "/api/v1/accounts/$id", replacement)
            assertEquals(created["id"], updated["id"])
            assertEquals("Updated", updated["name"].asString())
            assertEquals("SAVINGS", updated["accountType"].asString())
            assertEquals("GOAL", updated["purpose"].asString())
            assertEquals(500, updated["availableBalance"]["amount"].asInt())
            assertEquals(before.createdAt, accounts.findByIdAndUserId(id, userId)!!.createdAt)
            call("PUT", "/api/v1/accounts/$id", replacement)
            val conflictId = UUID.randomUUID().toString()
            call("POST", "/api/v1/accounts", accountBody(conflictId), 201)
            val conflict = call("PUT", "/api/v1/accounts/$id", replacement.replace(external, conflictId), 409)
            assertEquals("ACCOUNT_ALREADY_EXISTS", conflict["code"].asString())
            call("PUT", "/api/v1/accounts/$id", replacement.replace("BRL", "USD"), 400)
            call("PUT", "/api/v1/accounts/$id", replacement.replace(" Updated ", " "), 400)
            call("PUT", "/api/v1/accounts/$id", replacement.replace("500.00", "-1.00"), 400)
            call("PUT", "/api/v1/accounts/$id", "{}", 400)
            assertEquals("Updated", accounts.findByIdAndUserId(id, userId)!!.name)
            assertEquals(2, auditCount("ACCOUNT_UPDATED", id))
        }

        @Test
        fun `obligation PUT changes value date type and status without creating another record`() {
            val created = call("POST", "/api/v1/obligations", obligationBody, 201)
            val id = UUID.fromString(created["id"].asString())
            val before = obligations.findByIdAndUserId(id, userId)!!
            val body =
                obligationUpdate
                    .replace("Rent", " Energy ")
                    .replace("HOUSING", "UTILITIES")
                    .replace("1000.00", "150.00")
                    .replace("2026-09-25", "2026-10-15")
                    .replace("PENDING", "PAID")
            val updated = call("PUT", "/api/v1/obligations/$id", body)
            assertEquals(created["id"], updated["id"])
            assertEquals("Energy", updated["name"].asString())
            assertEquals("UTILITIES", updated["type"].asString())
            assertEquals("2026-10-15", updated["dueDate"].asString())
            assertEquals("PAID", updated["status"].asString())
            assertEquals(150, updated["amount"]["amount"].asInt())
            assertEquals(before.createdAt, obligations.findByIdAndUserId(id, userId)!!.createdAt)
            call("PUT", "/api/v1/obligations/$id", body)
            call("PUT", "/api/v1/obligations/$id", body.replace("150.00", "0.00"), 400)
            call("PUT", "/api/v1/obligations/$id", body.replace("2026-10-15", "2026-99-99"), 400)
            call("PUT", "/api/v1/obligations/$id", body.replace("PAID", "UNKNOWN"), 400)
            assertEquals("Energy", obligations.findByIdAndUserId(id, userId)!!.name)
            assertEquals(2, auditCount("OBLIGATION_UPDATED", id))
        }

        @Test
        fun `recurring obligation persists day advances after payment and appears in future plans`() {
            call("PUT", "/api/v1/profile", profileBody)
            call("POST", "/api/v1/accounts", accountBody(UUID.randomUUID().toString()), 201)
            val created =
                call(
                    "POST",
                    "/api/v1/obligations",
                    """{"name":"Rent","type":"HOUSING","amount":{"amount":1000.00},"recurring":true,"dueDay":31}""",
                    201,
                )
            val id = UUID.fromString(created["id"].asString())
            val first = LocalDate.parse(created["dueDate"].asString())
            assertEquals(31, created["dueDay"].asInt())
            assertTrue(created["recurring"].asBoolean())
            assertEquals(31.coerceAtMost(YearMonth.from(first).lengthOfMonth()), first.dayOfMonth)
            val paid = call("PATCH", "/api/v1/obligations/$id/paid")
            val next = YearMonth.from(first).plusMonths(1).atDay(31.coerceAtMost(YearMonth.from(first).plusMonths(1).lengthOfMonth()))
            assertEquals(next.toString(), paid["dueDate"].asString())
            assertEquals("PENDING", paid["status"].asString())
            assertEquals(next, obligations.findByIdAndUserId(id, userId)!!.dueDate)
            val plan = call("POST", "/api/v1/plans?asOf=${next.minusDays(1)}")
            assertEquals(1000, plan["committedObligations"]["amount"].asInt())
            call(
                "POST",
                "/api/v1/obligations",
                """{"name":"Invalid","type":"HOUSING","amount":{"amount":1000.00},"recurring":true,"dueDay":32}""",
                400,
            )
        }

        @Test
        fun `plan PUT recalculates in place reconciles actions and protects reviewed decisions`() {
            call("PUT", "/api/v1/profile", profileBody)
            call("POST", "/api/v1/accounts", accountBody(UUID.randomUUID().toString()), 201)
            call("POST", "/api/v1/obligations", obligationBody, 201)
            val created = call("POST", "/api/v1/plans?asOf=2026-09-23")
            val id = UUID.fromString(created["id"].asString())
            val oldObligationAction = created["actions"].first { it["type"].asString() == "RESERVE_FOR_OBLIGATIONS" }
            val originalCount = jdbc.queryForObject("select count(*) from financial_plans where user_id = ?", Int::class.java, userId)
            val body = """{"asOf":"2026-12-06"}"""
            val updated = call("PUT", "/api/v1/plans/$id", body)
            assertEquals(created["id"], updated["id"])
            assertEquals("2026-12-06", updated["asOf"].asString())
            assertEquals("2027-01-05", updated["nextIncomeDate"].asString())
            assertEquals(0, updated["committedObligations"]["amount"].asInt())
            assertNotEquals(created["dailySpendingLimit"], updated["dailySpendingLimit"])
            assertEquals(
                originalCount,
                jdbc.queryForObject("select count(*) from financial_plans where user_id = ?", Int::class.java, userId),
            )
            assertEquals(updated["asOf"], call("GET", "/api/v1/plans/latest")["asOf"])
            call("PATCH", "/api/v1/plans/actions/${oldObligationAction["id"].asString()}/approve", expected = 404)
            val replay = call("PUT", "/api/v1/plans/$id", body)
            val updatedIds = (0 until updated["actions"].size()).map { updated["actions"][it]["id"].asString() }.toSet()
            val replayIds = (0 until replay["actions"].size()).map { replay["actions"][it]["id"].asString() }.toSet()
            assertEquals(updatedIds, replayIds)
            call("PUT", "/api/v1/plans/$id", "{}", 400)
            call("PUT", "/api/v1/plans/$id", """{"asOf":"invalid"}""", 400)
            val action = replay["actions"].first { it["requiresApproval"].asBoolean() }
            call("PATCH", "/api/v1/plans/actions/${action["id"].asString()}/approve")
            val blocked = call("PUT", "/api/v1/plans/$id", """{"asOf":"2027-01-01"}""", 422)
            assertEquals("PLAN_ALREADY_REVIEWED", blocked["code"].asString())
            assertEquals("2026-12-06", plans.findByIdAndUserId(id, userId)!!.asOf.toString())
            assertEquals(2, auditCount("FINANCIAL_PLAN_UPDATED", id))
        }

        @Test
        fun `PUT refuses missing records and records owned by another user`() {
            val foreign = users.save(User(name = "Other", email = "${UUID.randomUUID()}@test.example", password = "unused"))
            val account = accounts.save(FinancialAccount(userId = foreign.id, externalId = UUID.randomUUID().toString()))
            val obligation = obligations.save(Obligation(userId = foreign.id))
            val plan = plans.save(FinancialPlan(userId = foreign.id))
            for ((resource, id, body) in listOf(
                Triple("accounts", account.id, accountBody("other")),
                Triple("obligations", obligation.id, obligationUpdate),
                Triple("plans", plan.id, """{"asOf":"2026-10-01"}"""),
            )) {
                call("PUT", "/api/v1/$resource/$id", body, 404)
                call("PUT", "/api/v1/$resource/${UUID.randomUUID()}", body, 404)
            }
            assertEquals(account.externalId, accounts.findByIdAndUserId(account.id, foreign.id)!!.externalId)
            assertEquals(obligation.name, obligations.findByIdAndUserId(obligation.id, foreign.id)!!.name)
            assertEquals(plan.id, plans.findByIdAndUserId(plan.id, foreign.id)!!.id)
            assertTrue(auditCount("FINANCIAL_PLAN_UPDATED", plan.id) == 0)
        }

        private fun auditCount(
            action: String,
            id: UUID,
        ): Int =
            jdbc.queryForObject(
                "select count(*) from audit_events where action = ? and resource_id = ? and user_id = ?",
                Int::class.java,
                action,
                id.toString(),
                userId,
            )!!

        private fun call(
            method: String,
            url: String,
            body: String? = null,
            expected: Int = 200,
        ): JsonNode {
            val builder = request(HttpMethod.valueOf(method), url)
            if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body)
            val response =
                mvc
                    .perform(builder)
                    .andExpect(status().`is`(expected))
                    .andReturn()
                    .response.contentAsString
            return json.readTree(response.ifBlank { "null" })
        }

        private fun accountBody(externalId: String) =
            """
            {"institution":"Bank","externalId":"$externalId","name":"Main","accountType":"CHECKING",
             "purpose":"OPERATING","availableBalance":{"amount":10000.00,"currency":"BRL"}}
            """.trimIndent()

        private val obligationBody =
            """
            {"name":"Rent","type":"HOUSING","amount":{"amount":1000.00},"dueDate":"2026-09-25"}
            """.trimIndent()
        private val obligationUpdate = obligationBody.dropLast(1) + ",\"status\":\"PENDING\"}"
        private val profileBody =
            """
            {"monthlyIncome":{"amount":7600.00},"payDay":5,"essentialMonthlyExpenses":{"amount":3500.00},
             "variableMonthlyBudget":{"amount":1200.00},"minimumCashBuffer":{"amount":500.00}}
            """.trimIndent()
    }
