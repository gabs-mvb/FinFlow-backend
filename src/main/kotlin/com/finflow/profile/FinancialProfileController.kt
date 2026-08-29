package com.finflow.profile

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/profile")
class FinancialProfileController(
    private val service: FinancialProfileService,
) {
    @PutMapping
    fun upsert(@Valid @RequestBody request: UpsertFinancialProfileRequest): FinancialProfileResponse =
        service.upsert(request)

    @GetMapping
    fun get(): FinancialProfileResponse = service.get()
}
