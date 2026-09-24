package com.finflow.transaction.adapter.inbound.http

import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import com.finflow.transaction.application.model.ImportTransactionItem
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class ImportTransactionItemBody(
    @field:NotBlank
    @field:Size(max = 180)
    val externalId: String,
    val type: TransactionType,
    @field:Valid
    val amount: MoneyInputBody,
    @field:NotBlank
    @field:Size(max = 300)
    val description: String,
    @field:Size(max = 180)
    val merchant: String? = null,
    val category: TransactionCategory? = null,
    val occurredAt: OffsetDateTime,
)

fun ImportTransactionItemBody.toCommand(): ImportTransactionItem =
    ImportTransactionItem(
        externalId = externalId,
        type = type,
        amount = amount.toCommand(),
        description = description,
        merchant = merchant,
        category = category,
        occurredAt = occurredAt,
    )
