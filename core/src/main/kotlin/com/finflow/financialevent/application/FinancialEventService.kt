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
import com.finflow.financialevent.domain.FinancialEventType
import com.finflow.onboarding.application.port.outbound.UserOnboardingRepository
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.shared.domain.ResourceNotFoundException
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Translates the privacy-preserving mobile event contract into the existing
 * canonical transaction import. Account ownership, user identity, database
 * deduplication and audit logging remain enforced by that use case.
 */
class FinancialEventService(
    private val transactions: FinancialTransactionUseCases,
    private val plans: FinancialPlanUseCases,
    private val clock: Clock,
    private val currentUser: CurrentUser,
    private val users: UserRepository,
    private val onboarding: UserOnboardingRepository,
    private val accounts: FinancialAccountUseCases,
    private val ledger: FinancialTransactionRepository,
) : FinancialEventUseCases {
    override fun importBatch(
        idempotencyKey: String,
        request: ImportFinancialEventsRequest,
    ): ImportFinancialEventsResponse {
        users.lockById(currentUser.id())
        val account = accounts.getRequired(request.accountId)
        val state = onboarding.findByUserId(currentUser.id())
        request.events.forEach { event ->
            require(event.type !in setOf(FinancialEventType.CREDIT_CARD_PURCHASE, FinancialEventType.CARD_PAYMENT) || event.cardLocalId != null) { "Selecione o cartão desta movimentação" }
            event.cardLocalId?.let { id ->
                val card = state?.data?.cards?.find { it.localId == id } ?: throw ResourceNotFoundException("Cartão não encontrado")
                require(card.institution.equals(account.institution, ignoreCase = true)) { "O cartão deve pertencer à instituição da conta" }
            }
        }
        val newEvents = request.events.distinctBy { it.externalId() }.filterNot { ledger.existsByAccountIdAndExternalId(account.id, it.externalId()) }
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
                                category = if (event.type in setOf(com.finflow.financialevent.domain.FinancialEventType.CARD_PAYMENT, com.finflow.financialevent.domain.FinancialEventType.TRANSFER_SENT, com.finflow.financialevent.domain.FinancialEventType.TRANSFER_RECEIVED)) com.finflow.transaction.domain.TransactionCategory.TRANSFER else null,
                            )
                        },
                    idempotencyOperation = FINANCIAL_EVENT_BATCH_OPERATION,
                    adjustTrackedBalance = true,
                    balanceExcludedExternalIds = request.events.filter { it.type == FinancialEventType.CREDIT_CARD_PURCHASE || (it.type == FinancialEventType.REFUND && it.cardLocalId != null) }.map { it.externalId() }.toSet(),
                ),
            )
        if (result.imported > 0 && state != null && newEvents.any { it.cardLocalId != null }) {
            val cards = state.data.cards.map { card ->
                val used = newEvents.filter { it.cardLocalId == card.localId }.fold(card.usedLimit) { balance, event ->
                    when (event.type) {
                        FinancialEventType.CREDIT_CARD_PURCHASE -> balance + event.amount.amount
                        FinancialEventType.CARD_PAYMENT, FinancialEventType.REFUND -> (balance - event.amount.amount).max(BigDecimal.ZERO)
                        else -> balance
                    }
                }
                card.copy(usedLimit = used)
            }
            onboarding.save(state.copy(data = state.data.copy(cards = cards), updatedAt = OffsetDateTime.now(clock)))
        }
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
