package com.finflow.shared.domain

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
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

    fun rounded(): Money = copy(
        amount = amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN),
    )

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Moedas incompatíveis" }
    }

    companion object {
        fun zero(currency: Currency): Money = Money(
            amount = BigDecimal.ZERO.setScale(currency.defaultFractionDigits),
            currency = currency,
        )
    }
}

data class MoneyInput(
    @field:DecimalMin("0.00")
    @field:Digits(integer = 17, fraction = 2)
    val amount: BigDecimal,
    @field:Pattern(regexp = "[A-Za-z]{3}")
    val currency: String = "BRL",
) {
    fun toMoney(): Money {
        val parsedCurrency = runCatching { Currency.getInstance(currency.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Moeda inválida: $currency") }
        return Money(amount, parsedCurrency)
    }
}

data class MoneyOutput(
    val amount: BigDecimal,
    val currency: String,
)

fun Money.toOutput(): MoneyOutput = MoneyOutput(
    amount = rounded().amount,
    currency = currency.currencyCode,
)
