package com.finflow.debt.application

import com.finflow.debt.application.model.DebtResponse
import com.finflow.debt.domain.Debt
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.domain.Money
import java.util.Currency

internal fun Debt.toResponse(): DebtResponse {
    val parsedCurrency = Currency.getInstance(currency)
    return DebtResponse(
        id,
        name,
        debtType,
        Money(outstandingAmount, parsedCurrency).toOutput(),
        Money(monthlyPayment, parsedCurrency).toOutput(),
        annualEffectiveRate,
        priority,
        status,
    )
}
