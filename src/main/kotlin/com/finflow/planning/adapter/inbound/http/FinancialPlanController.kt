package com.finflow.planning.adapter.inbound.http

import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/plans")
class FinancialPlanController(
    private val service: FinancialPlanUseCases,
    private val clock: Clock,
) {
    @PostMapping
    fun generate(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        asOf: LocalDate?,
    ): FinancialPlanResponse = service.generate(asOf ?: LocalDate.now(clock))

    @GetMapping("/latest")
    fun latest(): ResponseEntity<FinancialPlanResponse> =
        service
            .latest()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @PatchMapping("/actions/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
    ): ActionIntentResponse = service.reviewAction(id, true)

    @PatchMapping("/actions/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
    ): ActionIntentResponse = service.reviewAction(id, false)
}
