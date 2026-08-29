package com.finflow.transaction

import com.finflow.account.FinancialAccountService
import com.finflow.audit.AuditService
import com.finflow.shared.api.ConflictException
import com.finflow.shared.domain.Money
import com.finflow.shared.domain.MoneyInput
import com.finflow.shared.domain.MoneyOutput
import com.finflow.shared.domain.toOutput
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency
import java.util.UUID

data class ImportTransactionItem(
    @field:NotBlank
    @field:Size(max = 180)
    val externalId: String,
    val type: TransactionType,
    @field:Valid
    val amount: MoneyInput,
    @field:NotBlank
    @field:Size(max = 300)
    val description: String,
    @field:Size(max = 180)
    val merchant: String? = null,
    val category: TransactionCategory? = null,
    val occurredAt: OffsetDateTime,
)

data class ImportTransactionsRequest(
    val accountId: UUID,
    @field:Valid
    @field:Size(min = 1, max = 1000)
    val transactions: List<ImportTransactionItem>,
)

data class ImportTransactionsResponse(
    val imported: Int,
    val duplicates: Int,
    val idempotentReplay: Boolean,
)

data class FinancialTransactionResponse(
    val id: UUID,
    val accountId: UUID,
    val externalId: String,
    val type: TransactionType,
    val amount: MoneyOutput,
    val description: String,
    val merchant: String?,
    val category: TransactionCategory,
    val categorizationSource: CategorizationSource,
    val occurredAt: OffsetDateTime,
)

@Service
class FinancialTransactionService(
    private val repository: FinancialTransactionRepository,
    private val idempotencyRepository: IdempotencyRecordRepository,
    private val accountService: FinancialAccountService,
    private val categorizer: TransactionCategorizer,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun importTransactions(
        idempotencyKey: String,
        request: ImportTransactionsRequest,
    ): ImportTransactionsResponse {
        require(idempotencyKey.isNotBlank()) { "Idempotency-Key é obrigatório" }
        require(idempotencyKey.length <= 200) { "Idempotency-Key excede 200 caracteres" }
        val keyHash = sha256(idempotencyKey)
        val requestHash = sha256(request.toString())
        val operation = "TRANSACTION_IMPORT"
        idempotencyRepository.findByOperationAndKeyHash(operation, keyHash)?.let { record ->
            if (record.requestHash != requestHash) {
                throw ConflictException(
                    "A mesma chave de idempotência foi usada com outro payload",
                    "IDEMPOTENCY_KEY_REUSED",
                )
            }
            return ImportTransactionsResponse(
                imported = record.importedCount,
                duplicates = record.duplicateCount,
                idempotentReplay = true,
            )
        }

        val account = accountService.getRequired(request.accountId)
        val uniqueItems = request.transactions.distinctBy { it.externalId }
        var duplicates = request.transactions.size - uniqueItems.size
        val now = OffsetDateTime.now(clock)
        val newTransactions = uniqueItems.mapNotNull { item ->
            val money = item.amount.toMoney()
            require(money.isPositive()) { "O valor da transação deve ser maior que zero" }
            require(money.currency.currencyCode == account.currency) {
                "A moeda da transação deve ser igual à moeda da conta"
            }
            if (repository.existsByAccountIdAndExternalId(account.id, item.externalId)) {
                duplicates += 1
                null
            } else {
                val category = item.category ?: categorizer.categorize(item.description, item.merchant)
                FinancialTransactionEntity(
                    account = account,
                    externalId = item.externalId.trim(),
                    transactionType = item.type,
                    amount = money.amount,
                    currency = money.currency.currencyCode,
                    description = item.description.trim(),
                    merchant = item.merchant?.trim(),
                    category = category,
                    categorizationSource = if (item.category == null) {
                        CategorizationSource.RULE
                    } else {
                        CategorizationSource.PROVIDER
                    },
                    occurredAt = item.occurredAt,
                    importedAt = now,
                )
            }
        }
        repository.saveAll(newTransactions)
        idempotencyRepository.save(
            IdempotencyRecordEntity(
                operation = operation,
                keyHash = keyHash,
                requestHash = requestHash,
                importedCount = newTransactions.size,
                duplicateCount = duplicates,
                createdAt = now,
            ),
        )
        auditService.record(
            "TRANSACTIONS_IMPORTED",
            "FINANCIAL_ACCOUNT",
            account.id,
            "imported=${newTransactions.size};duplicates=$duplicates",
        )
        return ImportTransactionsResponse(newTransactions.size, duplicates, false)
    }

    @Transactional
    fun list(
        from: OffsetDateTime,
        to: OffsetDateTime,
        category: TransactionCategory?,
    ): List<FinancialTransactionResponse> {
        require(!to.isBefore(from)) { "O período final deve ser posterior ao inicial" }
        return repository.findAllByOccurredAtBetweenOrderByOccurredAtDesc(from, to)
            .asSequence()
            .filter { category == null || it.category == category }
            .map { it.toResponse() }
            .toList()
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

private fun FinancialTransactionEntity.toResponse(): FinancialTransactionResponse =
    FinancialTransactionResponse(
        id = id,
        accountId = account.id,
        externalId = externalId,
        type = transactionType,
        amount = Money(amount, Currency.getInstance(currency)).toOutput(),
        description = description,
        merchant = merchant,
        category = category,
        categorizationSource = categorizationSource,
        occurredAt = occurredAt,
    )

