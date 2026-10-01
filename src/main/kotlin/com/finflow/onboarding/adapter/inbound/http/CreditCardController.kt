package com.finflow.onboarding.adapter.inbound.http

import com.finflow.onboarding.application.port.inbound.ProgressiveOnboardingUseCases
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cards")
class CreditCardController(private val service: ProgressiveOnboardingUseCases) {
    @GetMapping fun list() = service.listCards()
    @PutMapping fun replace(@RequestBody body: CardsStepBody) = service.replaceCards(body.cards)
}
