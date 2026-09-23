package com.finflow.transaction

import com.finflow.account.FinancialAccountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class TransactionType {
    CREDIT,
    DEBIT,
}

enum class TransactionCategory {
    INCOME,
    HOUSING,
    FOOD,
    TRANSPORT,
    HEALTH,
    EDUCATION,
    SUBSCRIPTIONS,
    DEBT_PAYMENT,
    CREDIT_CARD,
    INVESTMENTS,
    LEISURE,
    TAXES,
    TRANSFER,
    OTHER,
}

enum class CategorizationSource {
    PROVIDER,
    RULE,
    USER,
}

@Entity
@Table(
    name = "financial_transactions",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_transaction_account_external",
        columnNames = ["account_id", "external_id"],
    )],
)
class FinancialTransactionEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: FinancialAccountEntity = FinancialAccountEntity(),
    @Column(name = "external_id", nullable = false, length = 180)
    var externalId: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    var transactionType: TransactionType = TransactionType.DEBIT,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3)
    var currency: String = "BRL",
    @Column(nullable = false, length = 300)
    var description: String = "",
    @Column(length = 180)
    var merchant: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var category: TransactionCategory = TransactionCategory.OTHER,
    @Enumerated(EnumType.STRING)
    @Column(name = "categorization_source", nullable = false, length = 24)
    var categorizationSource: CategorizationSource = CategorizationSource.RULE,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "imported_at", nullable = false)
    var importedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface FinancialTransactionRepository : JpaRepository<FinancialTransactionEntity, UUID> {
    fun existsByAccountIdAndExternalId(accountId: UUID, externalId: String): Boolean
    fun findAllByAccountUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        userId: Int,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<FinancialTransactionEntity>
}

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_idempotency_operation_key",
        columnNames = ["user_id", "operation", "key_hash"],
    )],
)
class IdempotencyRecordEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80)
    var operation: String = "",
    @Column(name = "key_hash", nullable = false, length = 64)
    var keyHash: String = "",
    @Column(name = "request_hash", nullable = false, length = 64)
    var requestHash: String = "",
    @Column(name = "imported_count", nullable = false)
    var importedCount: Int = 0,
    @Column(name = "duplicate_count", nullable = false)
    var duplicateCount: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecordEntity, UUID> {
    fun findByUserIdAndOperationAndKeyHash(userId: Int, operation: String, keyHash: String): IdempotencyRecordEntity?
}

