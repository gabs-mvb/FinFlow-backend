package com.finflow.openfinance

import com.finflow.authentication.domain.User
import com.finflow.authentication.domain.UserRepository
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.TestExecutionEvent
import org.springframework.security.test.context.support.WithUserDetails
import kotlin.test.Test
import kotlin.test.assertFalse

@SpringBootTest
@Transactional
@WithUserDetails(
    value = "consent-test@finflow.test",
    userDetailsServiceBeanName = "customUserDetailsService",
    setupBefore = TestExecutionEvent.TEST_EXECUTION,
)
class OpenFinanceConsentServiceTest @Autowired constructor(
    private val service: OpenFinanceConsentService,
    private val users: UserRepository,
) {
    @BeforeEach
    fun createUser() {
        users.save(User(
            name = "Consent test",
            email = "consent-test@finflow.test",
            password = "unused-test-password",
        ))
    }

    @Test
    fun `checks active consent through transactional proxy without null clock`() {
        assertFalse(service.hasActiveConsent())
    }
}
