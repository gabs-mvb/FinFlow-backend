package com.finflow.shared.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) {
    init {
        require(amount.scale() <= currency.defaultFractionDigits) {
            "Escala monetária inválida para ${currency.currencyCode}"
        }
    }

    fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount.add(other.amount))
    }

    fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount.subtract(other.amount))
    }

    fun isPositive(): Boolean = amount.compareTo(BigDecimal.ZERO) > 0

    fun isNegative(): Boolean = amount.compareTo(BigDecimal.ZERO) < 0

    fun nonNegative(): Money = if (isNegative()) zero(currency) else this

    fun rounded(): Money =
        copy(
            amount = amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN),
        )

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Moedas incompatíveis" }
    }

    companion object {
        fun zero(currency: Currency): Money =
            Money(
                amount = BigDecimal.ZERO.setScale(currency.defaultFractionDigits),
                currency = currency,
            )
    }
}
