package com.finflow.transaction

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/transactions")
class FinancialTransactionController(
    private val service: FinancialTransactionService,
) {
    @PostMapping("/imports")
    fun importTransactions(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: ImportTransactionsRequest,
    ): ImportTransactionsResponse = service.importTransactions(idempotencyKey, request)

    @GetMapping
    fun list(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: OffsetDateTime,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: OffsetDateTime,
        @RequestParam(required = false)
        category: TransactionCategory?,
    ): List<FinancialTransactionResponse> = service.list(from, to, category)
}
