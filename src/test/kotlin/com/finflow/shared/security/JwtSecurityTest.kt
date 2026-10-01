package com.finflow.shared.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val json: ObjectMapper,
    ) {
        @Test
        fun `health endpoint is public`() {
            mockMvc
                .get("/actuator/health")
                .andExpect { status { isOk() } }
        }

        @Test
        fun `business endpoint rejects missing jwt token`() {
            mockMvc
                .get("/api/v1/accounts")
                .andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `auth login endpoint is public`() {
            mockMvc
                .post("/api/auth/login")
                .andExpect { status { isBadRequest() } }
        }

        @Test
        fun `auth register endpoint is public`() {
            mockMvc
                .post("/api/auth/register")
                .andExpect { status { isBadRequest() } }
        }

        @Test
        fun `native login ignores old bearer and issued jwt authorizes own onboarding without origin or api key`() {
            val email = "native-${UUID.randomUUID()}@example.test"
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .servletPath("/api/auth/register")
                .header("Authorization", "Bearer invalid-old-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Android","email":"$email","password":"strong-test-password"}"""))
                .andExpect(status().isCreated)
            val response = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .servletPath("/api/auth/login")
                .header("Authorization", "Bearer invalid-old-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"strong-test-password"}"""))
                .andExpect(status().isOk).andReturn().response
            val token = json.readTree(response.contentAsString)["token"].asString()
            val onboarding = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/onboarding")
                .header("Authorization", "Bearer $token"))
                .andExpect(status().isOk).andReturn().response
            assertFalse(json.readTree(onboarding.contentAsString)["completed"].asBoolean())
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .servletPath("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"incorrect-password"}"""))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `invalid bearer cannot access current user or financial routes`() {
            listOf("/api/auth/me", "/api/v1/onboarding", "/api/v1/accounts").forEach { path ->
                mockMvc.perform(MockMvcRequestBuilders.get(path).servletPath(path)
                    .header("Authorization", "Bearer invalid-old-session"))
                    .andExpect(status().isUnauthorized)
            }
        }
    }
