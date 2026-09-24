package com.finflow.profile.adapter.inbound.http

import com.finflow.profile.application.model.FinancialProfileResponse
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/profile")
class FinancialProfileController(
    private val service: FinancialProfileUseCases,
) {
    @PutMapping
    fun upsert(
        @Valid @RequestBody request: UpsertFinancialProfileRequestBody,
    ): FinancialProfileResponse = service.upsert(request.toCommand())

    @GetMapping
    fun get(): FinancialProfileResponse = service.get()
}
