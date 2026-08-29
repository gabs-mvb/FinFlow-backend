package com.finflow.account

import com.finflow.audit.AuditService
import com.finflow.shared.api.ConflictException
import com.finflow.shared.api.ResourceNotFoundException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyInput
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

data class CreateAccountRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val institution: String,
    @field:NotBlank
    @field:Size(max = 160)
    val externalId: String,
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    val accountType: AccountType,
    val purpose: AccountPurpose,
    @field:Valid
    val availableBalance: MoneyInput,
    val lastSyncedAt: OffsetDateTime? = null,
)

data class UpdateAccountBalanceRequest(
    @field:Valid
    val availableBalance: MoneyInput,
    val syncedAt: OffsetDateTime? = null,
)

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

@Service
class FinancialAccountService(
    private val repository: FinancialAccountRepository,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateAccountRequest): FinancialAccountResponse {
        if (repository.existsByInstitutionIgnoreCaseAndExternalId(request.institution, request.externalId)) {
            throw ConflictException("A conta externa já foi cadastrada", "ACCOUNT_ALREADY_EXISTS")
        }
        val balance = request.availableBalance.toMoney()
        val now = OffsetDateTime.now(clock)
        val account = repository.save(
            FinancialAccountEntity(
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

    @Transactional
    fun updateBalance(id: UUID, request: UpdateAccountBalanceRequest): FinancialAccountResponse {
        val account = getRequired(id)
        val balance = request.availableBalance.toMoney()
        require(balance.currency.currencyCode == account.currency) { "A moeda da conta não pode ser alterada" }
        account.availableBalance = balance.amount
        account.lastSyncedAt = request.syncedAt ?: OffsetDateTime.now(clock)
        account.updatedAt = OffsetDateTime.now(clock)
        auditService.record("ACCOUNT_BALANCE_UPDATED", "FINANCIAL_ACCOUNT", account.id)
        return repository.save(account).toResponse()
    }

    @Transactional
    fun list(): List<FinancialAccountResponse> = repository.findAll().map { it.toResponse() }

    @Transactional
    fun getRequired(id: UUID): FinancialAccountEntity = repository.findById(id)
        .orElseThrow { ResourceNotFoundException("Conta não encontrada") }
}

private fun FinancialAccountEntity.toResponse(): FinancialAccountResponse = FinancialAccountResponse(
    id = id,
    institution = institution,
    externalId = externalId,
    name = name,
    accountType = accountType,
    purpose = purpose,
    availableBalance = Money(availableBalance, Currency.getInstance(currency)).toOutput(),
    lastSyncedAt = lastSyncedAt,
)

