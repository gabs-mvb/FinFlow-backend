package com.finflow.account.adapter.inbound.http

import com.finflow.account.application.model.UpdateAccountBalanceRequest
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import java.time.OffsetDateTime

data class UpdateAccountBalanceRequestBody(
    @field:Valid
    val availableBalance: MoneyInputBody,
    val syncedAt: OffsetDateTime? = null,
)

fun UpdateAccountBalanceRequestBody.toCommand(): UpdateAccountBalanceRequest =
    UpdateAccountBalanceRequest(
        availableBalance = availableBalance.toCommand(),
        syncedAt = syncedAt,
    )
