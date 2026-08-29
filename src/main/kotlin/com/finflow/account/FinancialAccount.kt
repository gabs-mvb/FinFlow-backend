package com.finflow.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class AccountType {
    CHECKING,
    SAVINGS,
    PAYMENT,
    INVESTMENT,
}

enum class AccountPurpose {
    OPERATING,
    EMERGENCY_RESERVE,
    GOAL,
    INVESTMENT,
}

@Entity
@Table(name = "financial_accounts")
class FinancialAccountEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 120)
    var institution: String = "",
    @Column(name = "external_id", nullable = false, length = 160)
    var externalId: String = "",
    @Column(nullable = false, length = 120)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    var accountType: AccountType = AccountType.CHECKING,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var purpose: AccountPurpose = AccountPurpose.OPERATING,
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    var availableBalance: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3)
    var currency: String = "BRL",
    @Column(name = "last_synced_at")
    var lastSyncedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface FinancialAccountRepository : JpaRepository<FinancialAccountEntity, UUID> {
    fun existsByInstitutionIgnoreCaseAndExternalId(institution: String, externalId: String): Boolean
    fun findAllByCurrency(currency: String): List<FinancialAccountEntity>
}

