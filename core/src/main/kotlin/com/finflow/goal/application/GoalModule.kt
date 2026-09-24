package com.finflow.goal.application

import com.finflow.goal.application.model.GoalResponse
import com.finflow.goal.domain.Goal
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.domain.Money
import java.util.Currency

internal fun Goal.toResponse(): GoalResponse {
    val parsedCurrency = Currency.getInstance(currency)
    return GoalResponse(
        id,
        name,
        Money(targetAmount, parsedCurrency).toOutput(),
        Money(currentAmount, parsedCurrency).toOutput(),
        targetDate,
        priority,
        status,
    )
}
