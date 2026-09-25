package com.finflow.composition

import com.finflow.account.application.FinancialAccountService
import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.FinancialAccountResponse
import com.finflow.account.application.model.UpdateAccountBalanceRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.application.port.inbound.FinancialAccountUseCases
import com.finflow.account.application.port.outbound.FinancialAccountRepository
import com.finflow.account.domain.FinancialAccount
import com.finflow.audit.application.AuditService
import com.finflow.audit.application.port.inbound.AuditUseCases
import com.finflow.audit.application.port.outbound.AuditEventRepository
import com.finflow.authentication.application.AuthenticationService
import com.finflow.authentication.application.model.LoginRequestDto
import com.finflow.authentication.application.model.LoginResponseDto
import com.finflow.authentication.application.model.RegisterRequestDto
import com.finflow.authentication.application.model.UserResponseDto
import com.finflow.authentication.application.port.inbound.AuthenticationUseCases
import com.finflow.authentication.application.port.outbound.CredentialAuthenticator
import com.finflow.authentication.application.port.outbound.PasswordHasher
import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.debt.application.DebtService
import com.finflow.debt.application.model.CreateDebtRequest
import com.finflow.debt.application.model.DebtResponse
import com.finflow.debt.application.port.inbound.DebtUseCases
import com.finflow.debt.application.port.outbound.DebtRepository
import com.finflow.goal.application.GoalService
import com.finflow.goal.application.model.CreateGoalRequest
import com.finflow.goal.application.model.GoalResponse
import com.finflow.goal.application.model.UpdateGoalProgressRequest
import com.finflow.goal.application.port.inbound.GoalUseCases
import com.finflow.goal.application.port.outbound.GoalRepository
import com.finflow.obligation.application.ObligationService
import com.finflow.obligation.application.model.CreateObligationRequest
import com.finflow.obligation.application.model.ObligationResponse
import com.finflow.obligation.application.model.UpdateObligationRequest
import com.finflow.obligation.application.port.inbound.ObligationUseCases
import com.finflow.obligation.application.port.outbound.ObligationRepository
import com.finflow.onboarding.application.OnboardingService
import com.finflow.onboarding.application.model.OnboardingStatus
import com.finflow.onboarding.application.port.inbound.OnboardingUseCases
import com.finflow.openfinance.application.OpenFinanceConsentService
import com.finflow.openfinance.application.model.ConsentResponse
import com.finflow.openfinance.application.model.OpenFinanceIntegrationStatus
import com.finflow.openfinance.application.model.RegisterConsentRequest
import com.finflow.openfinance.application.port.inbound.OpenFinanceConsentUseCases
import com.finflow.openfinance.application.port.outbound.OpenFinanceConsentRepository
import com.finflow.planning.application.FinancialPlanService
import com.finflow.planning.application.model.ActionIntentResponse
import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.UpdateFinancialPlanRequest
import com.finflow.planning.application.port.inbound.FinancialPlanUseCases
import com.finflow.planning.application.port.outbound.ActionIntentRepository
import com.finflow.planning.application.port.outbound.FinancialPlanRepository
import com.finflow.planning.application.port.outbound.PlanRevisionRepository
import com.finflow.portfolio.application.PortfolioService
import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import com.finflow.portfolio.application.port.outbound.PortfolioPositionRepository
import com.finflow.profile.application.FinancialProfileService
import com.finflow.profile.application.model.FinancialProfileResponse
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.application.port.inbound.FinancialProfileUseCases
import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.profile.domain.FinancialProfile
import com.finflow.report.application.MonthlyReportService
import com.finflow.report.application.model.MonthlyFinancialReport
import com.finflow.report.application.port.inbound.MonthlyReportUseCases
import com.finflow.shared.application.port.outbound.CurrentUser
import com.finflow.transaction.application.FinancialTransactionService
import com.finflow.transaction.application.model.FinancialTransactionResponse
import com.finflow.transaction.application.model.ImportTransactionsRequest
import com.finflow.transaction.application.model.ImportTransactionsResponse
import com.finflow.transaction.application.port.inbound.FinancialTransactionUseCases
import com.finflow.transaction.application.port.outbound.FinancialTransactionRepository
import com.finflow.transaction.application.port.outbound.IdempotencyRecordRepository
import com.finflow.transaction.domain.TransactionCategorizer
import com.finflow.transaction.domain.TransactionCategory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

@Configuration
class UseCaseConfiguration {
    @Bean
    fun transactions(manager: PlatformTransactionManager) = TransactionTemplate(manager)

    @Bean
    fun transactionCategorizer() = TransactionCategorizer()

    @Bean
    fun financialAccountService(
        currentUser: CurrentUser,
        repository: FinancialAccountRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): FinancialAccountUseCases {
        val target = FinancialAccountService(currentUser, repository, auditService, clock)
        return object : FinancialAccountUseCases {
            override fun update(
                id: UUID,
                request: UpdateAccountRequest,
            ): FinancialAccountResponse = inTransaction(transactions) { target.update(id, request) }

            override fun create(request: CreateAccountRequest): FinancialAccountResponse =
                inTransaction(transactions) { target.create(request) }

            override fun updateBalance(
                id: UUID,
                request: UpdateAccountBalanceRequest,
            ): FinancialAccountResponse = inTransaction(transactions) { target.updateBalance(id, request) }

            override fun list(): List<FinancialAccountResponse> = inTransaction(transactions) { target.list() }

            override fun getRequired(id: UUID): FinancialAccount = inTransaction(transactions) { target.getRequired(id) }
        }
    }

    @Bean
    fun auditService(
        currentUser: CurrentUser,
        repository: AuditEventRepository,
        clock: Clock,
        transactions: TransactionTemplate,
    ): AuditUseCases {
        val target = AuditService(currentUser, repository, clock)
        return object : AuditUseCases {
            override fun record(
                action: String,
                resourceType: String,
                resourceId: Any?,
                details: String?,
            ): Unit = inTransaction(transactions) { target.record(action, resourceType, resourceId, details) }
        }
    }

    @Bean
    fun authenticationService(
        currentUser: CurrentUser,
        userRepository: UserRepository,
        passwordEncoder: PasswordHasher,
        authenticator: CredentialAuthenticator,
        transactions: TransactionTemplate,
    ): AuthenticationUseCases {
        val target = AuthenticationService(currentUser, userRepository, passwordEncoder, authenticator)
        return object : AuthenticationUseCases {
            override fun me(): UserResponseDto = inTransaction(transactions) { target.me() }

            override fun login(request: LoginRequestDto): LoginResponseDto = inTransaction(transactions) { target.login(request) }

            override fun register(request: RegisterRequestDto): UserResponseDto = inTransaction(transactions) { target.register(request) }
        }
    }

    @Bean
    fun debtService(
        currentUser: CurrentUser,
        repository: DebtRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): DebtUseCases {
        val target = DebtService(currentUser, repository, auditService, clock)
        return object : DebtUseCases {
            override fun create(request: CreateDebtRequest): DebtResponse = inTransaction(transactions) { target.create(request) }

            override fun list(): List<DebtResponse> = inTransaction(transactions) { target.list() }

            override fun markPaid(id: UUID): DebtResponse = inTransaction(transactions) { target.markPaid(id) }
        }
    }

    @Bean
    fun goalService(
        currentUser: CurrentUser,
        repository: GoalRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): GoalUseCases {
        val target = GoalService(currentUser, repository, auditService, clock)
        return object : GoalUseCases {
            override fun create(request: CreateGoalRequest): GoalResponse = inTransaction(transactions) { target.create(request) }

            override fun list(): List<GoalResponse> = inTransaction(transactions) { target.list() }

            override fun updateProgress(
                id: UUID,
                request: UpdateGoalProgressRequest,
            ): GoalResponse = inTransaction(transactions) { target.updateProgress(id, request) }
        }
    }

    @Bean
    fun obligationService(
        currentUser: CurrentUser,
        repository: ObligationRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): ObligationUseCases {
        val target = ObligationService(currentUser, repository, auditService, clock)
        return object : ObligationUseCases {
            override fun update(
                id: UUID,
                request: UpdateObligationRequest,
            ): ObligationResponse = inTransaction(transactions) { target.update(id, request) }

            override fun create(request: CreateObligationRequest): ObligationResponse =
                inTransaction(transactions) { target.create(request) }

            override fun list(): List<ObligationResponse> = inTransaction(transactions) { target.list() }

            override fun markPaid(id: UUID): ObligationResponse = inTransaction(transactions) { target.markPaid(id) }
        }
    }

    @Bean
    fun onboardingService(
        events: com.finflow.onboarding.application.port.outbound.OnboardingEvents,
        currentUser: CurrentUser,
        profiles: FinancialProfileUseCases,
        users: UserRepository,
        transactions: TransactionTemplate,
    ): OnboardingUseCases {
        val target = OnboardingService(currentUser, profiles, users, events)
        return object : OnboardingUseCases {
            override fun status(): OnboardingStatus =
                com.finflow.onboarding.adapter.outbound.logging.OnboardingOperationLog.observe("status") {
                    inTransaction(transactions) { target.status() }
                }

            override fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus =
                com.finflow.onboarding.adapter.outbound.logging.OnboardingOperationLog.observe("complete") {
                    inTransaction(transactions) { target.complete(request) }
                }
        }
    }

    @Bean
    fun openFinanceConsentService(
        currentUser: CurrentUser,
        repository: OpenFinanceConsentRepository,
        auditService: AuditUseCases,
        clock: Clock,
        @Value("\${finflow.open-finance.provider}") configuredProvider: String,
        transactions: TransactionTemplate,
    ): OpenFinanceConsentUseCases {
        val target = OpenFinanceConsentService(currentUser, repository, auditService, clock, configuredProvider)
        return object : OpenFinanceConsentUseCases {
            override fun register(request: RegisterConsentRequest): ConsentResponse =
                inTransaction(transactions) { target.register(request) }

            override fun list(): List<ConsentResponse> = inTransaction(transactions) { target.list() }

            override fun hasActiveConsent(): Boolean = inTransaction(transactions) { target.hasActiveConsent() }

            override fun integrationStatus(): OpenFinanceIntegrationStatus = inTransaction(transactions) { target.integrationStatus() }
        }
    }

    @Bean
    fun financialPlanService(
        revisionRepository: PlanRevisionRepository,
        currentUser: CurrentUser,
        profileService: FinancialProfileUseCases,
        accountRepository: FinancialAccountRepository,
        transactionRepository: FinancialTransactionRepository,
        obligationRepository: ObligationRepository,
        debtRepository: DebtRepository,
        consentService: OpenFinanceConsentUseCases,
        planRepository: FinancialPlanRepository,
        actionRepository: ActionIntentRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): FinancialPlanUseCases {
        val target =
            FinancialPlanService(
                currentUser,
                profileService,
                accountRepository,
                transactionRepository,
                obligationRepository,
                debtRepository,
                consentService,
                planRepository,
                actionRepository,
                auditService,
                clock,
                revisionRepository,
            )
        return object : FinancialPlanUseCases {
            override fun preview(asOf: LocalDate): FinancialPlanResponse = inTransaction(transactions) { target.preview(asOf) }

            override fun update(
                id: UUID,
                request: UpdateFinancialPlanRequest,
            ): FinancialPlanResponse = inTransaction(transactions) { target.update(id, request) }

            override fun generate(asOf: LocalDate): FinancialPlanResponse = inTransaction(transactions) { target.generate(asOf) }

            override fun latest(): FinancialPlanResponse? = inTransaction(transactions) { target.latest() }

            override fun reviewAction(
                id: UUID,
                approve: Boolean,
            ): ActionIntentResponse = inTransaction(transactions) { target.reviewAction(id, approve) }
        }
    }

    @Bean
    fun portfolioService(
        currentUser: CurrentUser,
        positionRepository: PortfolioPositionRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): PortfolioUseCases {
        val target = PortfolioService(currentUser, positionRepository, auditService, clock)
        return object : PortfolioUseCases {
            override fun replace(request: ReplacePortfolioRequest): PortfolioResponse =
                inTransaction(transactions) { target.replace(request) }

            override fun get(): PortfolioResponse = inTransaction(transactions) { target.get() }

        }
    }

    @Bean
    fun financialProfileService(
        currentUser: CurrentUser,
        repository: FinancialProfileRepository,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): FinancialProfileUseCases {
        val target = FinancialProfileService(currentUser, repository, auditService, clock)
        return object : FinancialProfileUseCases {
            override fun upsert(request: UpsertFinancialProfileRequest): FinancialProfileResponse =
                inTransaction(transactions) { target.upsert(request) }

            override fun getRequired(): FinancialProfile = inTransaction(transactions) { target.getRequired() }

            override fun get(): FinancialProfileResponse = inTransaction(transactions) { target.get() }
        }
    }

    @Bean
    fun monthlyReportService(
        currentUser: CurrentUser,
        transactionRepository: FinancialTransactionRepository,
        profileService: FinancialProfileUseCases,
        transactions: TransactionTemplate,
    ): MonthlyReportUseCases {
        val target = MonthlyReportService(currentUser, transactionRepository, profileService)
        return object : MonthlyReportUseCases {
            override fun generate(period: YearMonth): MonthlyFinancialReport = inTransaction(transactions) { target.generate(period) }
        }
    }

    @Bean
    fun financialTransactionService(
        currentUser: CurrentUser,
        repository: FinancialTransactionRepository,
        idempotencyRepository: IdempotencyRecordRepository,
        accountService: FinancialAccountUseCases,
        categorizer: TransactionCategorizer,
        auditService: AuditUseCases,
        clock: Clock,
        transactions: TransactionTemplate,
    ): FinancialTransactionUseCases {
        val target =
            FinancialTransactionService(currentUser, repository, idempotencyRepository, accountService, categorizer, auditService, clock)
        return object : FinancialTransactionUseCases {
            override fun importTransactions(
                idempotencyKey: String,
                request: ImportTransactionsRequest,
            ): ImportTransactionsResponse = inTransaction(transactions) { target.importTransactions(idempotencyKey, request) }

            override fun list(
                from: OffsetDateTime,
                to: OffsetDateTime,
                category: TransactionCategory?,
            ): List<FinancialTransactionResponse> = inTransaction(transactions) { target.list(from, to, category) }
        }
    }
}

// Wrapping the value preserves nullable query results (e.g. no latest plan).
private fun <T> inTransaction(
    template: TransactionTemplate,
    action: () -> T,
): T = requireNotNull(template.execute { ResultBox(action()) }).value

private data class ResultBox<T>(
    val value: T,
)
