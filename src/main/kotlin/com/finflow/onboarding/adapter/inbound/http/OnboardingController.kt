package com.finflow.onboarding.adapter.inbound.http

import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.profile.adapter.inbound.http.UpsertFinancialProfileRequestBody
import com.finflow.profile.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController(
    private val service: OnboardingUseCases,
) {
    @GetMapping
    fun status(): OnboardingStatus = service.status()

    @PostMapping("/complete")
    fun complete(
        @Valid @RequestBody request: UpsertFinancialProfileRequestBody,
    ): OnboardingStatus = service.complete(request.toCommand())
}
