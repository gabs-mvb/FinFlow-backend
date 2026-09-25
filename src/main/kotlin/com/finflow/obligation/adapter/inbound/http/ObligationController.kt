package com.finflow.obligation.adapter.inbound.http

import com.finflow.obligation.application.model.ObligationResponse
import com.finflow.obligation.application.port.inbound.ObligationUseCases
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/obligations")
class ObligationController(
    private val service: ObligationUseCases,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateObligationRequestBody,
    ): ObligationResponse = service.create(request.toCommand())

    @GetMapping
    fun list(): List<ObligationResponse> = service.list()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateObligationRequestBody,
    ): ObligationResponse = service.update(id, request.toCommand())

    @PatchMapping("/{id}/paid")
    fun markPaid(
        @PathVariable id: UUID,
    ): ObligationResponse = service.markPaid(id)
}
