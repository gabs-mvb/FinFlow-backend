package com.finflow.account.adapter.inbound.http

import com.finflow.account.application.model.FinancialAccountResponse
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
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
@RequestMapping("/api/v1/accounts")
class FinancialAccountController(
    private val service: FinancialAccountUseCases,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateAccountRequestBody,
    ): FinancialAccountResponse = service.create(request.toCommand())

    @GetMapping
    fun list(): List<FinancialAccountResponse> = service.list()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountRequestBody,
    ): FinancialAccountResponse = service.update(id, request.toCommand())

    @PatchMapping("/{id}/balance")
    fun updateBalance(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountBalanceRequestBody,
    ): FinancialAccountResponse = service.updateBalance(id, request.toCommand())
}
