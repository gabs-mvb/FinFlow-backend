package com.finflow.shared.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
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
    }
