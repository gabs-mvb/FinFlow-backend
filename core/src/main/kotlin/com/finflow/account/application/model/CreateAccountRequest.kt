package com.finflow.account.application.model

import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.shared.application.model.MoneyInput
import java.time.OffsetDateTime

data class CreateAccountRequest(
    val institution: String,
    val externalId: String,
    val name: String,
    val accountType: AccountType,
    val purpose: AccountPurpose,
    val availableBalance: MoneyInput,
    val lastSyncedAt: OffsetDateTime? = null,
) {
    init {
        require(institution.isNotBlank()) { "institution não pode estar vazio" }
        require(institution.length in 0..120) { "Tamanho inválido para institution" }
        require(externalId.isNotBlank()) { "externalId não pode estar vazio" }
        require(externalId.length in 0..160) { "Tamanho inválido para externalId" }
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..120) { "Tamanho inválido para name" }
    }
}
