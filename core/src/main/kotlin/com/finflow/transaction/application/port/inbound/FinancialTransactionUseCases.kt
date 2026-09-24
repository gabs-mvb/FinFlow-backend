package com.finflow.transaction.application.port.inbound

import com.finflow.transaction.application.model.FinancialTransactionResponse
import com.finflow.transaction.application.model.ImportTransactionsRequest
import com.finflow.transaction.application.model.ImportTransactionsResponse
import com.finflow.transaction.domain.TransactionCategory
import java.time.OffsetDateTime

interface FinancialTransactionUseCases {
    fun importTransactions(
        idempotencyKey: String,
        request: ImportTransactionsRequest,
    ): ImportTransactionsResponse

    fun list(
        from: OffsetDateTime,
        to: OffsetDateTime,
        category: TransactionCategory?,
    ): List<FinancialTransactionResponse>
}
