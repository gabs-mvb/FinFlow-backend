package com.finflow.account.application.model

import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.shared.application.model.MoneyOutput
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialAccountResponse(
    val id: UUID,
    val institution: String,
    val externalId: String,
    val name: String,
    val accountType: AccountType,
    val purpose: AccountPurpose,
    val availableBalance: MoneyOutput,
    val lastSyncedAt: OffsetDateTime?,
)
