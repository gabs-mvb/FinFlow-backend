package com.finflow.account.application.model

import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.shared.application.model.MoneyInput
import java.time.OffsetDateTime

data class UpdateAccountRequest(
    val institution: String,
    val externalId: String,
    val name: String,
    val accountType: AccountType,
    val purpose: AccountPurpose,
    val availableBalance: MoneyInput,
    val lastSyncedAt: OffsetDateTime? = null,
) {
    init {
        require(institution.isNotBlank() && institution.length <= 120) { "Instituição inválida" }
        require(externalId.isNotBlank() && externalId.length <= 160) { "Identificador externo inválido" }
        require(name.isNotBlank() && name.length <= 120) { "Nome da conta inválido" }
    }
}
