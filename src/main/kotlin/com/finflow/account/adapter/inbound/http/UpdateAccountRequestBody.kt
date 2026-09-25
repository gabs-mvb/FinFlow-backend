package com.finflow.account.adapter.inbound.http

import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import com.finflow.shared.adapter.inbound.http.toCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class UpdateAccountRequestBody(
    @field:NotBlank @field:Size(max = 120) val institution: String,
    @field:NotBlank @field:Size(max = 160) val externalId: String,
    @field:NotBlank @field:Size(max = 120) val name: String,
    val accountType: AccountType,
    val purpose: AccountPurpose,
    @field:Valid val availableBalance: MoneyInputBody,
    val lastSyncedAt: OffsetDateTime? = null,
)

fun UpdateAccountRequestBody.toCommand(): UpdateAccountRequest =
    UpdateAccountRequest(
        institution,
        externalId,
        name,
        accountType,
        purpose,
        availableBalance.toCommand(),
        lastSyncedAt,
    )
