package com.finflow.financialevent.application

import com.finflow.financialevent.application.model.ImportFinancialEventsRequest
import com.finflow.financialevent.application.model.ImportFinancialEventsResponse
import com.finflow.financialevent.application.port.inbound.FinancialEventUseCases
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.domain.PlanSource
import com.finflow.shared.application.model.MoneyInput
import com.finflow.transaction.application.model.ImportTransactionItem
import com.finflow.transaction.application.model.ImportTransactionsRequest
import com.finflow.transaction.application.port.inbound.FinancialTransactionUseCases
import java.time.Clock
import java.time.LocalDate

/**
 * Translates the privacy-preserving mobile event contract into the existing
 * canonical transaction import. Account ownership, user identity, database
 * deduplication and audit logging remain enforced by that use case.
 */
class FinancialEventService(
    private val transactions: FinancialTransactionUseCases,
    private val plans: FinancialPlanUseCases,
    private val clock: Clock,
) : FinancialEventUseCases {
    override fun importBatch(
        idempotencyKey: String,
        request: ImportFinancialEventsRequest,
    ): ImportFinancialEventsResponse {
        val result =
            transactions.importTransactions(
                idempotencyKey,
                ImportTransactionsRequest(
                    accountId = request.accountId,
                    transactions =
                        request.events.map { event ->
                            ImportTransactionItem(
                                externalId = event.externalId(),
                                type = event.type.toTransactionType(),
                                amount = MoneyInput(event.amount.amount, event.amount.currency.uppercase()),
                                description = event.description.trim(),
                                merchant = event.merchant?.trim()?.ifBlank { null },
                                occurredAt = event.occurredAt,
                            )
                        },
                    idempotencyOperation = FINANCIAL_EVENT_BATCH_OPERATION,
                    adjustTrackedBalance = true,
                ),
            )
        // Existing rule-based plans are recalculated from the authoritative
        // transaction ledger and tracked balance. Personalized/manual plans are
        // never overwritten by an automated notification import.
        if (result.imported > 0 && plans.latest()?.details?.source == PlanSource.RULE_BASED) {
            plans.generate(LocalDate.now(clock))
        }
        return ImportFinancialEventsResponse(result.imported, result.duplicates, result.idempotentReplay)
    }

    private companion object {
        const val FINANCIAL_EVENT_BATCH_OPERATION = "FINANCIAL_EVENT_BATCH"
    }
}
