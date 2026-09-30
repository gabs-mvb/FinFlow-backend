package com.finflow.financialevent.domain

import com.finflow.transaction.domain.TransactionType

/**
 * Semantic types detected on the device. They deliberately remain separate
 * from the canonical transaction types persisted by the backend.
 */
enum class FinancialEventType {
    CREDIT_CARD_PURCHASE,
    DEBIT_PURCHASE,
    PIX_SENT,
    PIX_RECEIVED,
    TRANSFER_SENT,
    TRANSFER_RECEIVED,
    CARD_PAYMENT,
    BILL_PAYMENT,
    REFUND,
    WITHDRAWAL,
    INCOME,
    UNKNOWN;

    fun toTransactionType(): TransactionType =
        when (this) {
            CREDIT_CARD_PURCHASE,
            DEBIT_PURCHASE,
            PIX_SENT,
            TRANSFER_SENT,
            CARD_PAYMENT,
            BILL_PAYMENT,
            WITHDRAWAL -> TransactionType.DEBIT

            PIX_RECEIVED,
            TRANSFER_RECEIVED,
            REFUND,
            INCOME -> TransactionType.CREDIT

            UNKNOWN -> throw IllegalArgumentException("Eventos financeiros desconhecidos não podem ser sincronizados")
        }
}
