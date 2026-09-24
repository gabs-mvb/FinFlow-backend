package com.finflow.shared.application.model

import com.finflow.shared.domain.Money
import java.math.BigDecimal
import java.util.Currency

data class MoneyInput(
    val amount: BigDecimal,
    val currency: String = "BRL",
) {
    init {
        require(amount >= java.math.BigDecimal("0.00")) { "Valor inválido para amount" }
        require(amount.precision() - amount.scale() <= 17 && amount.scale() <= 2) { "Precisão monetária inválida" }
        require(currency.matches(Regex("[A-Za-z]{3}"))) { "Moeda inválida" }
    }

    fun toMoney(): Money {
        val parsedCurrency =
            runCatching { Currency.getInstance(currency.uppercase()) }
                .getOrElse { throw IllegalArgumentException("Moeda inválida: $currency") }
        return Money(amount, parsedCurrency)
    }
}
