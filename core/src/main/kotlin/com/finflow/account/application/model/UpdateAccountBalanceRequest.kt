package com.finflow.account.application.model

import com.finflow.shared.application.model.MoneyInput
import java.time.OffsetDateTime

data class UpdateAccountBalanceRequest(
    val availableBalance: MoneyInput,
    val syncedAt: OffsetDateTime? = null,
)
