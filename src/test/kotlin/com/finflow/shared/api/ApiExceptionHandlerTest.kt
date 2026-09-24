package com.finflow.shared.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class ApiExceptionHandlerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) {
        @Test
        @WithMockUser
        fun `returns a stable error for malformed json`() {
            mockMvc
                .post("/api/v1/accounts") {
                    contentType = MediaType.APPLICATION_JSON
                    content = "{\"institution\":"
                }.andExpect {
                    status { isBadRequest() }
                    content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                    jsonPath("$.code") { value("MALFORMED_REQUEST_BODY") }
                    jsonPath("$.detail") { value("O corpo da requisição não contém um JSON válido") }
                }
        }

        @Test
        @WithMockUser
        fun `identifies an invalid query parameter without exposing framework details`() {
            mockMvc
                .get("/api/v1/transactions") {
                    param("from", "2026-09-01T00:00:00Z")
                    param("to", "2026-09-20T00:00:00Z")
                    param("category", "UNKNOWN")
                }.andExpect {
                    status { isBadRequest() }
                    content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                    jsonPath("$.code") { value("INVALID_REQUEST") }
                    jsonPath("$.detail") { value("O valor informado para category é inválido") }
                }
        }
    }
