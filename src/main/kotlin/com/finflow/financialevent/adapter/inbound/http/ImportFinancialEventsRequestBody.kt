package com.finflow.financialevent.adapter.inbound.http

import com.finflow.financialevent.application.model.FinancialEventItem
import com.finflow.financialevent.application.model.ImportFinancialEventsRequest
import com.finflow.financialevent.domain.FinancialEventType
import com.finflow.shared.application.model.MoneyInput
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ImportFinancialEventsRequestBody(
    val accountId: UUID,
    @field:Size(min = 1, max = 1000)
    val events: List<@Valid FinancialEventItemBody>,
)

data class FinancialEventItemBody(
    @field:NotBlank
    @field:Size(max = 160)
    val fingerprint: String,
    val type: FinancialEventType,
    @field:DecimalMin("0.01")
    @field:Digits(integer = 17, fraction = 2)
    val amount: BigDecimal,
    @field:Pattern(regexp = "[A-Za-z]{3}")
    val currency: String,
    @field:NotBlank
    @field:Size(max = 300)
    val description: String,
    @field:Size(max = 180)
    val merchant: String? = null,
    val occurredAt: OffsetDateTime,
    val confidence: Double = 0.0,
    val confirmed: Boolean = false,
    val cardLocalId: UUID? = null,
)

fun ImportFinancialEventsRequestBody.toCommand(): ImportFinancialEventsRequest =
    ImportFinancialEventsRequest(
        accountId = accountId,
        events =
            events.map { event ->
                FinancialEventItem(
                    fingerprint = event.fingerprint,
                    type = event.type,
                    amount = MoneyInput(event.amount, event.currency),
                    description = event.description,
                    merchant = event.merchant,
                    occurredAt = event.occurredAt,
                    confidence = event.confidence,
                    confirmed = event.confirmed,
                    cardLocalId = event.cardLocalId,
                )
            },
    )
