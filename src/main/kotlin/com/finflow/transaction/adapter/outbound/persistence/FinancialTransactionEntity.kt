package com.finflow.transaction.adapter.outbound.persistence

import com.finflow.account.adapter.outbound.persistence.FinancialAccountEntity
import com.finflow.account.adapter.outbound.persistence.toDomain
import com.finflow.account.adapter.outbound.persistence.toEntity
import com.finflow.transaction.domain.CategorizationSource
import com.finflow.transaction.domain.TransactionCategory
import com.finflow.transaction.domain.TransactionType
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
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "financial_transactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_transaction_account_external",
            columnNames = ["account_id", "external_id"],
        ),
    ],
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
