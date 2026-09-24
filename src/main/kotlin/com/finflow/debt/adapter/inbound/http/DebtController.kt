package com.finflow.debt.adapter.inbound.http

import com.finflow.debt.application.model.DebtResponse
import com.finflow.debt.application.port.inbound.DebtUseCases
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
@RequestMapping("/api/v1/debts")
class DebtController(
    private val service: DebtUseCases,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateDebtRequestBody,
    ): DebtResponse = service.create(request.toCommand())

    @GetMapping fun list(): List<DebtResponse> = service.list()

    @PatchMapping("/{id}/paid")
    fun markPaid(
        @PathVariable id: UUID,
    ): DebtResponse = service.markPaid(id)
}
