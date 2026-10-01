package com.finflow

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.*
import java.util.UUID
import kotlin.test.*

@SpringBootTest
@AutoConfigureMockMvc
class ProgressiveOnboardingTest @Autowired constructor(private val mvc: MockMvc, private val json: ObjectMapper) {
    @Test fun `A completes with Nubank 2000 while B starts empty and cannot read update or delete A resources`() {
        val a = login()
        completeTestOnboarding(mvc, a, balance = 2000)
        val accountA = call("GET", "/api/v1/accounts", a).single()
        val idA = accountA["id"].asString()
        assertEquals(2000, accountA["availableBalance"]["amount"].asInt())
        assertTrue(call("GET", "/api/v1/onboarding", a)["readyForDashboard"].asBoolean())
        val b = login()
        val stateB = call("GET", "/api/v1/onboarding", b)
        assertEquals("NOT_STARTED", stateB["status"].asString())
        assertEquals("WELCOME", stateB["currentStep"].asString())
        assertEquals(0, stateB["data"]["accounts"].size())
        listOf("accounts", "cards", "goals", "obligations", "debts", "open-finance/consents").forEach { resource -> assertEquals(0, call("GET", "/api/v1/$resource", b).size()) }
        assertEquals(0, call("GET", "/api/v1/transactions?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z", b).size())
        call("GET", "/api/v1/accounts/$idA", b, expected = 404)
        call("PUT", "/api/v1/accounts/$idA", b, """{"institution":"Nubank","externalId":"x","name":"X","accountType":"CHECKING","purpose":"OPERATING","availableBalance":{"amount":1}}""", 404)
        call("PATCH", "/api/v1/accounts/$idA/balance", b, """{"availableBalance":{"amount":1}}""", 404)
        call("DELETE", "/api/v1/accounts/$idA", b, expected = 404)
        completeTestOnboarding(mvc, b, balance = 300)
        assertEquals(300, call("GET", "/api/v1/accounts", b).single()["availableBalance"]["amount"].asInt())
        assertEquals(2000, call("GET", "/api/v1/accounts", a).single()["availableBalance"]["amount"].asInt())
        val idB = call("GET", "/api/v1/accounts", b).single()["id"].asString()
        call("GET", "/api/v1/accounts/$idB", a, expected = 404)
        call("DELETE", "/api/v1/accounts/$idB", a, expected = 404)
    }
    @Test fun `steps persist across login retries do not duplicate accounts and completion cannot be bypassed`() {
        val token = login()
        call("POST", "/api/v1/onboarding/complete", token, expected = 400)
        call("PUT", "/api/v1/onboarding/income", token, """{"incomes":[{"amount":10,"schedule":"DAY_OF_MONTH","payDay":5}]}""", 400)
        call("POST", "/api/v1/onboarding/start", token)
        call("PUT", "/api/v1/onboarding/goal", token, """{"goals":["SAVE_MORE"],"userId":999999}""")
        assertEquals("INCOME", call("GET", "/api/v1/onboarding", token)["currentStep"].asString())
        call("PUT", "/api/v1/onboarding/income", token, """{"incomes":[{"amount":2500.25,"schedule":"VARIABLE"}]}""")
        call("PUT", "/api/v1/onboarding/institutions", token, """{"institutions":["Nubank"]}""")
        val local = UUID.randomUUID()
        val body = """{"accounts":[{"localId":"$local","institution":"Nubank","name":"Principal","type":"DIGITAL","balance":2000}]}"""
        call("PUT", "/api/v1/onboarding/accounts", token, body)
        call("PUT", "/api/v1/onboarding/accounts", token, body)
        assertEquals(1, call("GET", "/api/v1/accounts", token).size())
        assertEquals("CREDIT_CARDS", call("GET", "/api/v1/onboarding", token)["currentStep"].asString())
        assertFalse(call("GET", "/api/v1/onboarding", token)["completed"].asBoolean())
        call("POST", "/api/v1/onboarding/complete", token, expected = 400)
        val other = login()
        val foreign = call("GET", "/api/v1/accounts", token).single()["id"].asString()
        call("POST", "/api/v1/onboarding/start", other)
        call("PUT", "/api/v1/onboarding/goal", other, """{"goals":["ORGANIZE"]}""")
        call("PUT", "/api/v1/onboarding/income", other, """{"incomes":[{"amount":0,"schedule":"VARIABLE"}]}""")
        call("PUT", "/api/v1/onboarding/institutions", other, """{"institutions":["Nubank"]}""")
        call("PUT", "/api/v1/onboarding/accounts", other, body.replace("\"balance\":2000", "\"balance\":2000,\"accountId\":\"$foreign\""), 400)
        assertEquals(0, call("GET", "/api/v1/accounts", other).size())
    }
    @Test fun `low confidence cannot create transactions and confirmed card purchase never debits cash twice`() {
        val token = login()
        call("POST", "/api/v1/onboarding/start", token)
        call("PUT", "/api/v1/onboarding/goal", token, """{"goals":["ORGANIZE"]}""")
        call("PUT", "/api/v1/onboarding/income", token, """{"incomes":[{"amount":5000,"schedule":"DAY_OF_MONTH","payDay":5}]}""")
        call("PUT", "/api/v1/onboarding/institutions", token, """{"institutions":["Nubank"]}""")
        call("PUT", "/api/v1/onboarding/accounts", token, """{"accounts":[{"localId":"${UUID.randomUUID()}","institution":"Nubank","name":"Principal","type":"CHECKING","balance":2000}]}""")
        val card = UUID.randomUUID()
        call("PUT", "/api/v1/onboarding/cards", token, """{"cards":[{"localId":"$card","institution":"Nubank","name":"Cartão","limit":3000,"closingDay":5,"dueDay":10,"usedLimit":0}]}""")
        call("PUT", "/api/v1/onboarding/profile", token, """{"monthlySavings":100,"reserveStatus":"PARTIAL","reserveAmount":500,"expenses":[]}""")
        call("PUT", "/api/v1/onboarding/automation", token, """{"enabled":true}""")
        call("POST", "/api/v1/onboarding/complete", token)
        assertTrue(call("POST", "/api/v1/onboarding/complete", token)["completed"].asBoolean())
        val account = call("GET", "/api/v1/accounts", token).single()["id"].asString()
        val event = """{"accountId":"$account","events":[{"fingerprint":"purchase","type":"CREDIT_CARD_PURCHASE","amount":89.90,"currency":"BRL","description":"Compra","occurredAt":"2026-09-25T12:00:00Z","confidence":0.84,"confirmed":false,"cardLocalId":"$card"}]}"""
        call("POST", "/api/v1/financial-events/batch", token, event, 400, "purchase")
        val confirmed = event.replace("\"confirmed\":false", "\"confirmed\":true")
        call("POST", "/api/v1/financial-events/batch", token, confirmed, key = "purchase")
        call("POST", "/api/v1/financial-events/batch", token, confirmed, key = "retry-purchase")
        assertEquals(2000, call("GET", "/api/v1/accounts", token).single()["availableBalance"]["amount"].asInt())
        assertEquals(89.9, call("GET", "/api/v1/onboarding", token)["data"]["cards"][0]["usedLimit"].asDouble())
        val payment = confirmed.replace("\"purchase\"", "\"payment\"").replace("CREDIT_CARD_PURCHASE", "CARD_PAYMENT")
        call("POST", "/api/v1/financial-events/batch", token, payment, key = "payment")
        assertEquals(1910.1, call("GET", "/api/v1/accounts", token).single()["availableBalance"]["amount"].asDouble())
        assertEquals(0, call("GET", "/api/v1/onboarding", token)["data"]["cards"][0]["usedLimit"].asInt())
        val updatedCard = """{"cards":[{"localId":"$card","institution":"Nubank","name":"Novo nome","limit":4000,"closingDay":6,"dueDay":11,"usedLimit":0}]}"""
        call("PUT", "/api/v1/cards", token, updatedCard)
        assertEquals("Novo nome", call("GET", "/api/v1/cards", token).single()["name"].asString())
        val other = login()
        assertEquals(0, call("GET", "/api/v1/cards", other).size())
        call("PUT", "/api/v1/cards", other, updatedCard, 403)
    }
    private fun login(): String {
        val email = "onboarding-${UUID.randomUUID()}@example.test"
        call("POST", "/api/auth/register", body = """{"name":"Onboarding","email":"$email","password":"strong-test-password"}""", expected = 201)
        return call("POST", "/api/auth/login", body = """{"email":"$email","password":"strong-test-password"}""")["token"].asString()
    }
    private fun call(method: String, path: String, token: String? = null, body: String? = null, expected: Int = 200, key: String? = null): JsonNode {
        val builder = request(HttpMethod.valueOf(method), path)
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (key != null) builder.header("Idempotency-Key", key)
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body)
        return json.readTree(mvc.perform(builder).andExpect(status().`is`(expected)).andReturn().response.contentAsString.ifBlank { "null" })
    }
}
