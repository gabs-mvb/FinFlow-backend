package com.finflow.transaction

import com.finflow.account.AccountPurpose
import com.finflow.account.AccountType
import com.finflow.account.CreateAccountRequest
import com.finflow.account.FinancialAccountService
import com.finflow.shared.api.ConflictException
import com.finflow.shared.domain.MoneyInput
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class FinancialTransactionServiceTest @Autowired constructor(
    private val accountService: FinancialAccountService,
    private val transactionService: FinancialTransactionService,
) {
    @Test
    fun `replays the original result for the same idempotency key and payload`() {
        val account = createAccount("idempotency-account")
        val request = importRequest(account.id, "transaction-1", "Supermercado")

        val first = transactionService.importTransactions("key-1", request)
        val replay = transactionService.importTransactions("key-1", request)

        assertEquals(1, first.imported)
        assertEquals(1, replay.imported)
        assertTrue(replay.idempotentReplay)
    }

    @Test
    fun `rejects reuse of an idempotency key with a different payload`() {
        val account = createAccount("conflict-account")
        transactionService.importTransactions(
            "key-conflict",
            importRequest(account.id, "transaction-1", "Supermercado"),
        )

        assertFailsWith<ConflictException> {
            transactionService.importTransactions(
                "key-conflict",
                importRequest(account.id, "transaction-2", "Posto de combustível"),
            )
        }
    }

    private fun createAccount(externalId: String) = accountService.create(
        CreateAccountRequest(
            institution = "Sandbox Bank",
            externalId = externalId,
            name = "Conta principal",
            accountType = AccountType.CHECKING,
            purpose = AccountPurpose.OPERATING,
            availableBalance = MoneyInput(BigDecimal("1000.00")),
        ),
    )

    private fun importRequest(accountId: java.util.UUID, externalId: String, description: String) =
        ImportTransactionsRequest(
            accountId = accountId,
            transactions = listOf(
                ImportTransactionItem(
                    externalId = externalId,
                    type = TransactionType.DEBIT,
                    amount = MoneyInput(BigDecimal("100.00")),
                    description = description,
                    occurredAt = OffsetDateTime.parse("2026-07-15T12:00:00Z"),
                ),
            ),
        )
}

