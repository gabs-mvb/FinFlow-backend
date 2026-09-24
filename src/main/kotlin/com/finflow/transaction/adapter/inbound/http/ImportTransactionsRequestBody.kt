package com.finflow.transaction.adapter.inbound.http

import com.finflow.transaction.application.model.ImportTransactionsRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.UUID

data class ImportTransactionsRequestBody(
    val accountId: UUID,
    @field:Valid
    @field:Size(min = 1, max = 1000)
    val transactions: List<ImportTransactionItemBody>,
)

fun ImportTransactionsRequestBody.toCommand(): ImportTransactionsRequest =
    ImportTransactionsRequest(
        accountId = accountId,
        transactions = transactions.map { it.toCommand() },
    )
