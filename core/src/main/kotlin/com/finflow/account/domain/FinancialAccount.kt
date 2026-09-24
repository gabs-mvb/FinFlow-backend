package com.finflow.account.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class FinancialAccount(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val institution: String = "",
    val externalId: String = "",
    val name: String = "",
    val accountType: AccountType = AccountType.CHECKING,
    val purpose: AccountPurpose = AccountPurpose.OPERATING,
    val availableBalance: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val lastSyncedAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
