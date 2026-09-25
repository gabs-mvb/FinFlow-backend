package com.finflow.obligation.application

import com.finflow.obligation.application.model.ObligationResponse
import com.finflow.obligation.domain.Obligation
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.domain.Money
import java.util.Currency

internal fun Obligation.toResponse(): ObligationResponse =
    ObligationResponse(
        id = id,
        name = name,
        type = obligationType,
        amount = Money(amount, Currency.getInstance(currency)).toOutput(),
        dueDate = dueDate,
        recurring = recurring,
        dueDay = dueDay,
        status = status,
    )
