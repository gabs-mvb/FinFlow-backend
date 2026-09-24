package com.finflow.planning.adapter.inbound.http

import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PersonalizedPlanRequest
import com.finflow.planning.application.model.PlanRevisionResponse
import com.finflow.planning.application.port.inbound.EditablePlanningUseCases
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.application.port.inbound.PersonalizedPlanningUseCases
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val personalized: PersonalizedPlanningUseCases,
    private val editable: EditablePlanningUseCases,
    @Value("\${finflow.planning.ai.enabled:false}") private val aiEnabled: Boolean,
) {
    @PostMapping
    fun generate(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        asOf: LocalDate?,
    ): FinancialPlanResponse =
        if (aiEnabled) {
            personalized.generate(PersonalizedPlanRequest(asOf ?: LocalDate.now(clock)))
        } else {
            service.generate(asOf ?: LocalDate.now(clock))
        }

    @PostMapping("/personalized")
    fun personalize(
        @RequestBody request: PersonalizedPlanRequest,
    ): FinancialPlanResponse = personalized.generate(request)

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): FinancialPlanResponse = editable.get(id)

    @PutMapping("/{id}/content")
    fun replace(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReplacePlanContentRequestBody,
    ): FinancialPlanResponse = editable.replace(id, request.toCommand())

    @GetMapping("/{id}/revisions")
    fun revisions(
        @PathVariable id: UUID,
    ): List<PlanRevisionResponse> = editable.revisions(id)

    @GetMapping("/latest")
    fun latest(): ResponseEntity<FinancialPlanResponse> =
        service
            .latest()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateFinancialPlanRequestBody,
    ): FinancialPlanResponse = service.update(id, request.toCommand())

    @PatchMapping("/actions/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
    ): ActionIntentResponse = service.reviewAction(id, true)

    @PatchMapping("/actions/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
    ): ActionIntentResponse = service.reviewAction(id, false)
}
