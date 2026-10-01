package com.finflow

import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/** Real HTTP journey; no test-only completion flags. */
fun completeTestOnboarding(mvc: MockMvc, token: String, income: Int = 6000, balance: Int = 0) {
    fun call(method: String, path: String, body: String? = null) {
        val builder = request(HttpMethod.valueOf(method), "/api/v1/onboarding$path").header("Authorization", "Bearer $token")
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body)
        mvc.perform(builder).andExpect(status().isOk)
    }
    call("POST", "/start")
    call("PUT", "/goal", """{"goals":["ORGANIZE"]}""")
    call("PUT", "/income", """{"incomes":[{"name":"Principal","amount":$income,"schedule":"DAY_OF_MONTH","payDay":5}]}""")
    call("PUT", "/institutions", """{"institutions":["Nubank"]}""")
    call("PUT", "/accounts", """{"accounts":[{"localId":"${UUID.randomUUID()}","institution":"Nubank","name":"Onboarding","type":"CHECKING","balance":$balance}]}""")
    call("PUT", "/cards", """{"cards":[]}""")
    call("PUT", "/profile", """{"monthlySavings":0,"reserveStatus":"NONE","reserveAmount":0,"expenses":[]}""")
    call("PUT", "/automation", """{"enabled":false}""")
    call("POST", "/complete")
}
