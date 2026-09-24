package com.finflow

import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.AccountPurpose
import com.finflow.account.domain.AccountType
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.UpdateObligationRequest
import com.finflow.obligation.application.port.inbound.ObligationUseCases
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.planning.application.model.UpdateFinancialPlanRequest
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.shared.application.model.MoneyInput
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.TestExecutionEvent
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
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
        private val obligationUseCases: ObligationUseCases,
        private val obligationRepository: ObligationRepository,
        private val planUseCases: FinancialPlanUseCases,
        private val planRepository: FinancialPlanRepository,
        private val actions: ActionIntentRepository,
        private val profileUseCases: FinancialProfileUseCases,
        private val transactions: TransactionTemplate,
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
            val before = accountRepository.findAllByUserId(userId)
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
            assertEquals(before, accountRepository.findAllByUserId(userId))
        }

        @Test
        fun `full updates roll back when their audit event fails`() {
            reset(audit)
            val account =
                accounts.create(
                    CreateAccountRequest(
                        "Bank",
                        UUID.randomUUID().toString(),
                        "Main",
                        AccountType.CHECKING,
                        AccountPurpose.OPERATING,
                        MoneyInput(BigDecimal("10000.00")),
                    ),
                )
            val obligation =
                obligationUseCases.create(
                    CreateObligationRequest(
                        "Rent",
                        ObligationType.HOUSING,
                        MoneyInput(BigDecimal("1000.00")),
                        LocalDate.of(2026, 9, 25),
                    ),
                )
            profileUseCases.upsert(
                UpsertFinancialProfileRequest(
                    MoneyInput(BigDecimal("7600.00")),
                    5,
                    MoneyInput(BigDecimal("3500.00")),
                    MoneyInput(BigDecimal("1200.00")),
                    MoneyInput(BigDecimal("500.00")),
                ),
            )
            val plan = planUseCases.generate(LocalDate.of(2026, 9, 23))
            val beforeAccount = accountRepository.findByIdAndUserId(account.id, userId)
            val beforeObligation = obligationRepository.findByIdAndUserId(obligation.id, userId)
            val beforePlan = planRepository.findByIdAndUserId(plan.id, userId)
            val beforeActions = transactions.execute { actions.findAllByPlanIdAndPlanUserId(plan.id, userId).toSet() }
            doThrow(IllegalStateException("Audit storage unavailable"))
                .`when`(audit)
                .record(anyString(), anyString(), any(), nullable(String::class.java))
            assertFailsWith<IllegalStateException> {
                accounts.update(
                    account.id,
                    UpdateAccountRequest(
                        "Bank",
                        account.externalId,
                        "Changed",
                        AccountType.SAVINGS,
                        AccountPurpose.GOAL,
                        MoneyInput(BigDecimal("1.00")),
                    ),
                )
            }
            assertFailsWith<IllegalStateException> {
                obligationUseCases.update(
                    obligation.id,
                    UpdateObligationRequest(
                        "Changed",
                        ObligationType.OTHER,
                        MoneyInput(BigDecimal("1.00")),
                        LocalDate.of(2026, 10, 1),
                        ObligationStatus.CANCELLED,
                    ),
                )
            }
            assertFailsWith<IllegalStateException> {
                planUseCases.update(plan.id, UpdateFinancialPlanRequest(LocalDate.of(2026, 12, 6)))
            }
            assertEquals(beforeAccount, accountRepository.findByIdAndUserId(account.id, userId))
            assertEquals(beforeObligation, obligationRepository.findByIdAndUserId(obligation.id, userId))
            assertEquals(beforePlan, planRepository.findByIdAndUserId(plan.id, userId))
            assertEquals(beforeActions, transactions.execute { actions.findAllByPlanIdAndPlanUserId(plan.id, userId).toSet() })
        }

        @Test
        fun `onboarding and profile share one atomic transaction`() {
            val before = profiles.findByUserId(userId)
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
            assertEquals(before, profiles.findByUserId(userId))
            assertFalse(users.findByEmail("rollback@finflow.test").orElseThrow().onboardingCompleted)
        }
    }
