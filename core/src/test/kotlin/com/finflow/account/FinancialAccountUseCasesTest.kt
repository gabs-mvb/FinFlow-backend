package com.finflow.account

import com.finflow.account.application.FinancialAccountService
import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.UpdateAccountBalanceRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.account.domain.FinancialAccount
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.authentication.domain.User
import com.finflow.shared.application.model.MoneyInput
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.shared.domain.ConflictException
import com.finflow.shared.domain.ResourceNotFoundException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FinancialAccountUseCasesTest {
    private val repository = MemoryAccounts()
    private val events = mutableListOf<String>()
    private val clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC)
    private val service =
        FinancialAccountService(
            object : CurrentUser {
                override fun user() = User(7, "Owner", "owner@example.test", "unused")
            },
            repository,
            object : AuditUseCases {
                override fun record(
                    action: String,
                    resourceType: String,
                    resourceId: Any?,
                    details: String?,
                ) {
                    events += action
                }
            },
            clock,
        )

    @Test
    fun `normalizes external identity and rejects a duplicate without another audit event`() {
        service.create(request())
        assertFailsWith<ConflictException> { service.create(request().copy(institution = "bank")) }
        assertEquals(
            "external-1",
            repository.values.values
                .single()
                .externalId,
        )
        assertEquals(listOf("ACCOUNT_CREATED"), events)
    }

    @Test
    fun `updates a detached account without mutating the previous domain snapshot`() {
        val created = service.create(request())
        val snapshot = repository.values.getValue(created.id)
        val response = service.updateBalance(created.id, UpdateAccountBalanceRequest(MoneyInput(BigDecimal("75.00"))))
        assertEquals(BigDecimal("100.00"), snapshot.availableBalance)
        assertEquals(BigDecimal("75.00"), response.availableBalance.amount)
        assertEquals(OffsetDateTime.now(clock), response.lastSyncedAt)
        assertEquals(listOf("ACCOUNT_CREATED", "ACCOUNT_BALANCE_UPDATED"), events)
    }

    @Test
    fun `cannot read or change another users account`() {
        val foreign = FinancialAccount(userId = 8, availableBalance = BigDecimal("200.00"))
        repository.save(foreign)
        assertEquals(emptyList(), service.list())
        assertFailsWith<ResourceNotFoundException> {
            service.updateBalance(foreign.id, UpdateAccountBalanceRequest(MoneyInput(BigDecimal("0.00"))))
        }
        assertEquals(foreign, repository.values.getValue(foreign.id))
        assertEquals(emptyList(), events)
    }

    @Test
    fun `validates application commands without a web framework`() {
        assertFailsWith<IllegalArgumentException> { request().copy(name = " ") }
        assertFailsWith<IllegalArgumentException> { MoneyInput(BigDecimal("-1.00")) }
        val created = service.create(request())
        assertFailsWith<IllegalArgumentException> {
            service.updateBalance(created.id, UpdateAccountBalanceRequest(MoneyInput(BigDecimal("10.00"), "USD")))
        }
        assertEquals(BigDecimal("100.00"), repository.values.getValue(created.id).availableBalance)
    }

    @Test
    fun `full update preserves ownership and creation time and rejects another accounts identity`() {
        val first = service.create(request())
        service.create(request().copy(externalId = "external-2"))
        val previous = repository.values.getValue(first.id)
        val update =
            UpdateAccountRequest(
                "bank",
                "external-1",
                " Savings ",
                AccountType.SAVINGS,
                AccountPurpose.GOAL,
                MoneyInput(BigDecimal("50.00")),
            )
        val changed = service.update(first.id, update)
        assertEquals("Savings", changed.name)
        assertEquals(previous.createdAt, repository.values.getValue(first.id).createdAt)
        assertEquals(previous.userId, repository.values.getValue(first.id).userId)
        assertEquals("Main", previous.name)
        assertFailsWith<ConflictException> { service.update(first.id, update.copy(externalId = "external-2")) }
        assertEquals("external-1", repository.values.getValue(first.id).externalId)
        assertEquals(listOf("ACCOUNT_CREATED", "ACCOUNT_CREATED", "ACCOUNT_UPDATED"), events)
    }

    private fun request() =
        CreateAccountRequest(
            " Bank ",
            " external-1 ",
            " Main ",
            AccountType.CHECKING,
            AccountPurpose.OPERATING,
            MoneyInput(BigDecimal("100.00")),
        )
}

private class MemoryAccounts : FinancialAccountRepository {
    val values = linkedMapOf<UUID, FinancialAccount>()

    override fun save(value: FinancialAccount): FinancialAccount = value.also { values[it.id] = it }

    override fun existsByUserIdAndInstitutionIgnoreCaseAndExternalId(
        userId: Int,
        institution: String,
        externalId: String,
    ) = values.values.any { it.userId == userId && it.institution.equals(institution, true) && it.externalId == externalId }

    override fun findAllByUserId(userId: Int) = values.values.filter { it.userId == userId }

    override fun findByIdAndUserId(
        id: UUID,
        userId: Int,
    ) = values[id]?.takeIf { it.userId == userId }

    override fun findAllByUserIdAndCurrency(
        userId: Int,
        currency: String,
    ) = findAllByUserId(userId).filter { it.currency == currency }
}
