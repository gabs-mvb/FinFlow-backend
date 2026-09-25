package com.finflow

import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.FinancialAccount
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.Obligation
import com.finflow.planning.application.model.PlanProposal
import com.finflow.planning.application.model.PlanningEvidence
import com.finflow.planning.application.model.ReplacePlanContentRequest
import com.finflow.planning.application.port.outbound.PlanAdvisor
import com.finflow.planning.domain.ActionType
import com.finflow.planning.domain.PlanActionDraft
import com.finflow.planning.domain.PlanContent
import com.finflow.planning.domain.RiskLevel
import com.finflow.shared.domain.AiPlanningException
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.domain.FinancialTransaction
import com.finflow.transaction.domain.TransactionCategory
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.TestExecutionEvent
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@SpringBootTest(properties = ["finflow.planning.ai.enabled=true"])
@AutoConfigureMockMvc
@WithUserDetails(value = "personalized@finflow.test", setupBefore = TestExecutionEvent.TEST_EXECUTION)
class PersonalizedPlanningTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val json: ObjectMapper,
        private val jdbc: JdbcTemplate,
        private val users: UserRepository,
        private val accounts: FinancialAccountRepository,
        private val transactions: FinancialTransactionRepository,
        private val obligations: ObligationRepository,
        private val transactionTemplate: TransactionTemplate,
    ) {
        @MockitoBean private lateinit var advisor: PlanAdvisor
        private var owner = 0
        private lateinit var account: FinancialAccount
        private var captured: PlanningEvidence? = null

        @BeforeEach
        fun setup() {
            owner =
                users
                    .findByEmail("personalized@finflow.test")
                    .orElseGet {
                        users.save(User(name = "Private name", email = "personalized@finflow.test", password = "unused"))
                    }.id
            jdbc.update("delete from action_intents where plan_id in (select id from financial_plans where user_id = ?)", owner)
            for (table in listOf(
                "financial_plan_revisions",
                "financial_plans",
                "obligations",
            )) {
                jdbc.update("delete from $table where user_id = ?", owner)
            }
            jdbc.update(
                "delete from financial_transactions where account_id in (select id from financial_accounts where user_id = ?)",
                owner,
            )
            jdbc.update("delete from financial_accounts where user_id = ?", owner)
            account =
                accounts.save(
                    FinancialAccount(userId = owner, externalId = UUID.randomUUID().toString(), availableBalance = BigDecimal("10000.00")),
                )
            reset(advisor)
            doAnswer { call ->
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Network must not hold a transaction")
                val evidence = call.getArgument<PlanningEvidence>(0)
                captured = evidence
                PlanProposal(proposal(evidence), "test-model", "v1")
            }.`when`(advisor).suggest(matching<PlanningEvidence>(), anyString())
        }

        private fun <T> matching(): T = any<T>()

        private fun profile() =
            call(
                "PUT",
                "/api/v1/profile",
                """
                {"monthlyIncome":{"amount":7600},"payDay":5,"essentialMonthlyExpenses":{"amount":3500},
                 "variableMonthlyBudget":{"amount":1200},"minimumCashBuffer":{"amount":500}}
                """.trimIndent(),
            )

        private fun proposal(evidence: PlanningEvidence) =
            evidence.baseline.copy(
                summary = "Plano personalizado",
                analysis = "Priorizar reserva e reduzir gastos dispensáveis.",
                debtPaymentRecommendation = BigDecimal.ZERO,
                reserveContribution = BigDecimal("100.00"),
                investmentContribution = BigDecimal.ZERO,
                dailySpendingLimit = BigDecimal.ZERO,
                allocations = emptyList(),
                actions =
                    listOf(
                        PlanActionDraft(
                            ActionType.TRANSFER_TO_EMERGENCY_RESERVE,
                            BigDecimal("100.00"),
                            RiskLevel.LOW,
                            "Aporte ajustado ao perfil",
                        ),
                    ),
            )

        @Test
        fun `AI receives scoped aggregates and saves an editable proposal outside network transaction`() {
            profile()
            transactionTemplate.executeWithoutResult {
                transactions.save(
                    FinancialTransaction(
                        account = account,
                        externalId = UUID.randomUUID().toString(),
                        amount = BigDecimal("125.00"),
                        category = TransactionCategory.FOOD,
                        description = "PRIVATE DESCRIPTION",
                        merchant = "PRIVATE MERCHANT",
                        occurredAt = OffsetDateTime.parse("2026-09-20T12:00:00Z"),
                    ),
                )
            }
            val foreignUser = users.save(User(name = "Other", email = "${UUID.randomUUID()}@test.example", password = "unused"))
            val foreignAccount = accounts.save(FinancialAccount(userId = foreignUser.id, externalId = UUID.randomUUID().toString()))
            transactionTemplate.executeWithoutResult {
                transactions.save(
                    FinancialTransaction(
                        account = foreignAccount,
                        externalId = UUID.randomUUID().toString(),
                        amount = BigDecimal("99999.00"),
                        category = TransactionCategory.FOOD,
                        occurredAt = OffsetDateTime.parse("2026-09-20T12:00:00Z"),
                    ),
                )
            }
            val plan = call("POST", "/api/v1/plans?asOf=2026-09-24")
            assertEquals("AI", plan["details"]["source"].asString())
            assertEquals(1, captured!!.transactionCount)
            assertEquals(
                0,
                captured!!
                    .spendingByCategory
                    .single()
                    .total
                    .compareTo(BigDecimal("125")),
            )
            val evidenceJson = json.writeValueAsString(captured)
            for (privateValue in listOf(
                "PRIVATE",
                "personalized@",
                "99999",
                account.id.toString(),
            )) {
                assertFalse(evidenceJson.contains(privateValue))
            }
            assertEquals(plan["content"], call("GET", "/api/v1/plans/${plan["id"].asString()}")["content"])
            assertEquals(plan["content"], call("GET", "/api/v1/plans/latest")["content"])
        }

        @Test
        fun `full edit resets approvals archives content and refuses stale revisions and other owners`() {
            profile()
            val plan = call("POST", "/api/v1/plans/personalized", """{"asOf":"2026-09-24","preferences":"Reserva primeiro"}""")
            val id = plan["id"].asString()
            val oldAction = plan["actions"][0]["id"].asString()
            call("PATCH", "/api/v1/plans/actions/$oldAction/approve")
            val content =
                json.treeToValue(plan["content"], PlanContent::class.java).copy(
                    summary = "Minha escolha",
                    warnings = listOf("Revisar gastos | manter educação"),
                    reserveContribution = BigDecimal("200.00"),
                    actions =
                        listOf(
                            PlanActionDraft(ActionType.TRANSFER_TO_EMERGENCY_RESERVE, BigDecimal("200.00"), RiskLevel.LOW, "Minha reserva"),
                        ),
                )
            val body = json.writeValueAsString(ReplacePlanContentRequest(0, content))
            call("PUT", "/api/v1/plans/$id/content", json.writeValueAsString(mapOf("content" to content)), 400)
            val edited = call("PUT", "/api/v1/plans/$id/content", body)
            assertEquals(1, edited["revision"].asInt())
            assertEquals("MANUAL", edited["details"]["source"].asString())
            assertEquals("Revisar gastos | manter educação", call("GET", "/api/v1/plans/$id")["warnings"][0].asString())
            assertEquals("PROPOSED", edited["actions"][0]["status"].asString())
            assertNotEquals(oldAction, edited["actions"][0]["id"].asString())
            call("PATCH", "/api/v1/plans/actions/$oldAction/approve", expected = 404)
            call("PUT", "/api/v1/plans/$id/content", body, 409)
            val history = call("GET", "/api/v1/plans/$id/revisions")
            assertEquals("APPROVED", history[0]["plan"]["actions"][0]["status"].asString())
            assertEquals(plan["content"], history[0]["plan"]["content"])
            val invalid = json.writeValueAsString(ReplacePlanContentRequest(1, content.copy(reserveContribution = BigDecimal("999999.00"))))
            call("PUT", "/api/v1/plans/$id/content", invalid, 400)
            assertEquals(1, call("GET", "/api/v1/plans/$id/revisions").size())
            val foreign = users.save(User(name = "Other", email = "${UUID.randomUUID()}@test.example", password = "unused"))
            jdbc.update("update financial_plans set user_id = ? where id = ?", foreign.id, UUID.fromString(id))
            call("GET", "/api/v1/plans/$id", expected = 404)
            call("GET", "/api/v1/plans/$id/revisions", expected = 404)
            call("PUT", "/api/v1/plans/$id/content", body, 404)
        }

        @Test
        fun `provider failures invalid proposals and changed evidence do not save partial plans`() {
            profile()
            doAnswer { throw AiPlanningException("Unavailable") }.`when`(advisor).suggest(matching<PlanningEvidence>(), anyString())
            call("POST", "/api/v1/plans?asOf=2026-09-24", expected = 503)
            doAnswer { call ->
                PlanProposal(proposal(call.getArgument(0)).copy(reserveContribution = BigDecimal("999999.00")), "test", "v1")
            }.`when`(advisor)
                .suggest(matching<PlanningEvidence>(), anyString())
            call("POST", "/api/v1/plans?asOf=2026-09-24", expected = 503)
            doAnswer { call ->
                accounts.save(account.copy(availableBalance = BigDecimal("9000.00")))
                PlanProposal(proposal(call.getArgument(0)), "test", "v1")
            }.`when`(advisor).suggest(matching<PlanningEvidence>(), anyString())
            call("POST", "/api/v1/plans?asOf=2026-09-24", expected = 409)
            assertEquals(0, jdbc.queryForObject("select count(*) from financial_plans where user_id = ?", Int::class.java, owner))
        }

        @Test
        fun `overdue commitments remain protected when date advances`() {
            profile()
            obligations.save(Obligation(userId = owner, amount = BigDecimal("9500.00"), dueDate = LocalDate.parse("2026-08-01")))
            call("POST", "/api/v1/plans?asOf=2026-09-24", expected = 503)
            assertEquals(0, jdbc.queryForObject("select count(*) from financial_plans where user_id = ?", Int::class.java, owner))
        }

        private fun call(
            method: String,
            url: String,
            body: String? = null,
            expected: Int = 200,
        ): JsonNode {
            val request = request(HttpMethod.valueOf(method), url)
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body)
            val result =
                mvc
                    .perform(request)
                    .andExpect(status().`is`(expected))
                    .andReturn()
                    .response.contentAsString
            return json.readTree(result.ifBlank { "null" })
        }
    }
