package com.finflow.account

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
@RequestMapping("/api/v1/accounts")
class FinancialAccountController(
    private val service: FinancialAccountService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateAccountRequest): FinancialAccountResponse =
        service.create(request)

    @GetMapping
    fun list(): List<FinancialAccountResponse> = service.list()

    @PatchMapping("/{id}/balance")
    fun updateBalance(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountBalanceRequest,
    ): FinancialAccountResponse = service.updateBalance(id, request)
}
