package com.finflow.shared.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeySecurityTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `health endpoint is public`() {
        mockMvc.get("/actuator/health")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `business endpoint rejects missing api key`() {
        mockMvc.get("/api/v1/accounts")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `business endpoint accepts configured api key`() {
        mockMvc.get("/api/v1/accounts") {
            header("X-API-Key", "test-api-key")
        }.andExpect { status { isOk() } }
    }
}
