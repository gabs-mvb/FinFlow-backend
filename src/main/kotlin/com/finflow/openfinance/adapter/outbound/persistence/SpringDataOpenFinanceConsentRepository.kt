package com.finflow.openfinance.adapter.outbound.persistence

import com.finflow.openfinance.domain.ConsentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataOpenFinanceConsentRepository : JpaRepository<OpenFinanceConsentEntity, UUID> {
    fun findByUserIdAndProviderIgnoreCaseAndExternalConsentId(
        userId: Int,
        provider: String,
        externalConsentId: String,
    ): OpenFinanceConsentEntity?

    fun findAllByUserId(userId: Int): List<OpenFinanceConsentEntity>

    fun findAllByUserIdAndStatus(
        userId: Int,
        status: ConsentStatus,
    ): List<OpenFinanceConsentEntity>
}
