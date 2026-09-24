package com.finflow

import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.shared.application.model.MoneyInput
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.TestExecutionEvent
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

@SpringBootTest
@WithUserDetails(value = "rollback@finflow.test", setupBefore = TestExecutionEvent.TEST_EXECUTION)
class TransactionBoundaryTest
    @Autowired
    constructor(
        private val accounts: FinancialAccountUseCases,
        private val accountRepository: FinancialAccountRepository,
        private val onboarding: OnboardingUseCases,
        private val profiles: FinancialProfileRepository,
        private val users: UserRepository,
    ) {
        @MockitoBean
        private lateinit var audit: AuditUseCases

        private var userId: Int = 0

        @BeforeEach
        fun arrange() {
            userId =
                users
                    .findByEmail("rollback@finflow.test")
                    .orElseGet {
                        users.save(User(name = "Rollback", email = "rollback@finflow.test", password = "unused"))
                    }.id
            doThrow(IllegalStateException("Audit storage unavailable"))
                .`when`(audit)
                .record(anyString(), anyString(), any(), nullable(String::class.java))
        }

        @Test
        fun `failed audit rolls back the account through the input port`() {
            assertFailsWith<IllegalStateException> {
                accounts.create(
                    CreateAccountRequest(
                        "Bank",
                        "rollback-account",
                        "Main",
                        AccountType.CHECKING,
                        AccountPurpose.OPERATING,
                        MoneyInput(BigDecimal("100.00")),
                    ),
                )
            }
            assertEquals(emptyList(), accountRepository.findAllByUserId(userId))
        }

        @Test
        fun `onboarding and profile share one atomic transaction`() {
            assertFailsWith<IllegalStateException> {
                onboarding.complete(
                    UpsertFinancialProfileRequest(
                        monthlyIncome = MoneyInput(BigDecimal("1000.00")),
                        payDay = 5,
                        essentialMonthlyExpenses = MoneyInput(BigDecimal("500.00")),
                        variableMonthlyBudget = MoneyInput(BigDecimal("100.00")),
                        minimumCashBuffer = MoneyInput(BigDecimal("50.00")),
                    ),
                )
            }
            assertNull(profiles.findByUserId(userId))
            assertFalse(users.findByEmail("rollback@finflow.test").orElseThrow().onboardingCompleted)
        }
    }
