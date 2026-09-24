package com.finflow.shared.application.model

import com.finflow.shared.domain.Money

fun Money.toOutput(): MoneyOutput =
    MoneyOutput(
        amount = rounded().amount,
        currency = currency.currencyCode,
    )
