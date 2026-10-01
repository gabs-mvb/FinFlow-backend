package com.finflow.financialevent.application.model

import com.finflow.financialevent.domain.FinancialEventType
import com.finflow.shared.application.model.MoneyInput
import java.time.OffsetDateTime

data class FinancialEventItem(
    val fingerprint: String,
    val type: FinancialEventType,
    val amount: MoneyInput,
    val description: String,
    val merchant: String? = null,
    val occurredAt: OffsetDateTime,
    val confidence: Double = 0.0,
    val confirmed: Boolean = false,
    val cardLocalId: java.util.UUID? = null,
) {
    init {
        require(fingerprint.isNotBlank()) { "fingerprint não pode estar vazio" }
        require(fingerprint.trim().length <= MAX_FINGERPRINT_LENGTH) { "Fingerprint excede $MAX_FINGERPRINT_LENGTH caracteres" }
        require(description.isNotBlank()) { "description não pode estar vazio" }
        require(description.length <= 300) { "Tamanho inválido para description" }
        require(merchant == null || merchant.length <= 180) { "Tamanho inválido para merchant" }
        require(amount.toMoney().isPositive()) { "O valor do evento deve ser maior que zero" }
        require(type != FinancialEventType.UNKNOWN) { "Eventos financeiros desconhecidos não podem ser sincronizados" }
        require(confidence.isFinite() && confidence in 0.0..1.0) { "Confiança inválida" }
        require(confirmed || (confidence >= 0.95 && type != FinancialEventType.CREDIT_CARD_PURCHASE)) { "Confirme a movimentação antes de sincronizar" }
    }

    /**
     * The fingerprint is the stable event identity. Namespacing keeps mobile
     * events distinct from external IDs supplied by other import adapters.
     */
    fun externalId(): String = "$EXTERNAL_ID_PREFIX${fingerprint.trim()}"

    private companion object {
        const val EXTERNAL_ID_PREFIX = "financial-event:"
        const val MAX_FINGERPRINT_LENGTH = 160
    }
}
