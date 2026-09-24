package com.finflow.account.application

import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.FinancialAccountResponse
import com.finflow.account.application.model.UpdateAccountBalanceRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.FinancialAccount
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ConflictException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.ResourceNotFoundException
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

class FinancialAccountService(
    private val currentUser: CurrentUser,
    private val repository: FinancialAccountRepository,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : FinancialAccountUseCases {
    override fun create(request: CreateAccountRequest): FinancialAccountResponse {
        if (repository.existsByUserIdAndInstitutionIgnoreCaseAndExternalId(
                currentUser.id(),
                request.institution.trim(),
                request.externalId.trim(),
            )
        ) {
            throw ConflictException("A conta externa já foi cadastrada", "ACCOUNT_ALREADY_EXISTS")
        }
        val balance = request.availableBalance.toMoney()
        val now = OffsetDateTime.now(clock)
        val account =
            repository.save(
                FinancialAccount(
                    userId = currentUser.id(),
                    institution = request.institution.trim(),
                    externalId = request.externalId.trim(),
                    name = request.name.trim(),
                    accountType = request.accountType,
                    purpose = request.purpose,
                    availableBalance = balance.amount,
                    currency = balance.currency.currencyCode,
                    lastSyncedAt = request.lastSyncedAt,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        auditService.record("ACCOUNT_CREATED", "FINANCIAL_ACCOUNT", account.id)
        return account.toResponse()
    }

    override fun update(
        id: UUID,
        request: UpdateAccountRequest,
    ): FinancialAccountResponse {
        val account = getRequired(id)
        val institution = request.institution.trim()
        val externalId = request.externalId.trim()
        val identityChanged = !account.institution.equals(institution, ignoreCase = true) || account.externalId != externalId
        if (identityChanged && repository.existsByUserIdAndInstitutionIgnoreCaseAndExternalId(currentUser.id(), institution, externalId)) {
            throw ConflictException("A conta externa já foi cadastrada", "ACCOUNT_ALREADY_EXISTS")
        }
        val balance = request.availableBalance.toMoney()
        require(balance.currency.currencyCode == account.currency) { "A moeda da conta não pode ser alterada" }
        val saved =
            repository.save(
                account.copy(
                    institution = institution,
                    externalId = externalId,
                    name = request.name.trim(),
                    accountType = request.accountType,
                    purpose = request.purpose,
                    availableBalance = balance.amount,
                    lastSyncedAt = request.lastSyncedAt,
                    updatedAt = OffsetDateTime.now(clock),
                ),
            )
        auditService.record("ACCOUNT_UPDATED", "FINANCIAL_ACCOUNT", id)
        return saved.toResponse()
    }

    override fun updateBalance(
        id: UUID,
        request: UpdateAccountBalanceRequest,
    ): FinancialAccountResponse {
        val account = getRequired(id)
        val balance = request.availableBalance.toMoney()
        require(balance.currency.currencyCode == account.currency) { "A moeda da conta não pode ser alterada" }
        val updated =
            account.copy(
                availableBalance = balance.amount,
                lastSyncedAt = request.syncedAt ?: OffsetDateTime.now(clock),
                updatedAt = OffsetDateTime.now(clock),
            )
        auditService.record("ACCOUNT_BALANCE_UPDATED", "FINANCIAL_ACCOUNT", account.id)
        return repository.save(updated).toResponse()
    }

    override fun list(): List<FinancialAccountResponse> = repository.findAllByUserId(currentUser.id()).map { it.toResponse() }

    override fun getRequired(id: UUID): FinancialAccount =
        repository.findByIdAndUserId(id, currentUser.id())
            ?: throw ResourceNotFoundException("Conta não encontrada")
}

private fun FinancialAccount.toResponse(): FinancialAccountResponse =
    FinancialAccountResponse(
        id = id,
        institution = institution,
        externalId = externalId,
        name = name,
        accountType = accountType,
        purpose = purpose,
        availableBalance = Money(availableBalance, Currency.getInstance(currency)).toOutput(),
        lastSyncedAt = lastSyncedAt,
    )
