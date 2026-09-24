package com.finflow.transaction.application

import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.shared.application.model.toOutput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ConflictException
import com.finflow.shared.domain.Money
import com.finflow.transaction.application.model.FinancialTransactionResponse
import com.finflow.transaction.application.model.ImportTransactionsRequest
import com.finflow.transaction.application.model.ImportTransactionsResponse
import com.finflow.transaction.application.port.inbound.FinancialTransactionUseCases
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.application.port.outbound.IdempotencyRecordRepository
import com.finflow.transaction.domain.CategorizationSource
import com.finflow.transaction.domain.FinancialTransaction
import com.finflow.transaction.domain.IdempotencyRecord
import com.finflow.transaction.domain.TransactionCategorizer
import com.finflow.transaction.domain.TransactionCategory
import java.security.MessageDigest
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Currency

class FinancialTransactionService(
    private val currentUser: CurrentUser,
    private val repository: FinancialTransactionRepository,
    private val idempotencyRepository: IdempotencyRecordRepository,
    private val accountService: FinancialAccountUseCases,
    private val categorizer: TransactionCategorizer,
    private val auditService: AuditUseCases,
    private val clock: Clock,
) : FinancialTransactionUseCases {
    /**
     * Importa um lote uma única vez por chave de idempotência. Itens repetidos
     * no lote ou já associados à conta entram na contagem de duplicados.
     */
    override fun importTransactions(
        idempotencyKey: String,
        request: ImportTransactionsRequest,
    ): ImportTransactionsResponse {
        require(idempotencyKey.isNotBlank()) { "Idempotency-Key é obrigatório" }
        require(idempotencyKey.length <= 200) { "Idempotency-Key excede 200 caracteres" }
        val account = accountService.getRequired(request.accountId)
        val keyHash = sha256(idempotencyKey)
        val requestHash = sha256(request.toString())
        val operation = "TRANSACTION_IMPORT"
        idempotencyRepository.findByUserIdAndOperationAndKeyHash(currentUser.id(), operation, keyHash)?.let { record ->
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
        val uniqueItems = request.transactions.distinctBy { it.externalId }
        var duplicates = request.transactions.size - uniqueItems.size
        val now = OffsetDateTime.now(clock)
        val newTransactions =
            uniqueItems.mapNotNull { item ->
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
                    FinancialTransaction(
                        account = account,
                        externalId = item.externalId.trim(),
                        transactionType = item.type,
                        amount = money.amount,
                        currency = money.currency.currencyCode,
                        description = item.description.trim(),
                        merchant = item.merchant?.trim(),
                        category = category,
                        categorizationSource =
                            if (item.category == null) {
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
            IdempotencyRecord(
                userId = currentUser.id(),
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

    /** Lista transações no intervalo fechado, com filtro opcional de categoria. */
    override fun list(
        from: OffsetDateTime,
        to: OffsetDateTime,
        category: TransactionCategory?,
    ): List<FinancialTransactionResponse> {
        require(!to.isBefore(from)) { "O período final deve ser posterior ao inicial" }
        return repository
            .findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(currentUser.id(), from, to)
            .asSequence()
            .filter { category == null || it.category == category }
            .map { it.toResponse() }
            .toList()
    }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}

private fun FinancialTransaction.toResponse(): FinancialTransactionResponse =
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
