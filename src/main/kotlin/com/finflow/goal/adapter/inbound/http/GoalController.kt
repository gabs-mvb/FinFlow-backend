package com.finflow.goal.adapter.inbound.http

import com.finflow.goal.application.model.GoalResponse
import com.finflow.goal.application.port.inbound.GoalUseCases
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/goals")
class GoalController(
    private val service: GoalUseCases,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateGoalRequestBody,
    ): GoalResponse = service.create(request.toCommand())

    @GetMapping fun list(): List<GoalResponse> = service.list()

    @PatchMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateGoalProgressRequestBody,
    ): GoalResponse = service.updateProgress(id, request.toCommand())
}
