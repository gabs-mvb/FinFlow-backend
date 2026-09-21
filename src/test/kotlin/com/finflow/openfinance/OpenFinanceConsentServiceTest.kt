package com.finflow.openfinance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertFalse

@SpringBootTest
class OpenFinanceConsentServiceTest @Autowired constructor(
    private val service: OpenFinanceConsentService,
) {
    @Test
    fun `checks active consent through transactional proxy without null clock`() {
        assertFalse(service.hasActiveConsent())
    }
}
