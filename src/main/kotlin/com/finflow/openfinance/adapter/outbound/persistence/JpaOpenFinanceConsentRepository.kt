package com.finflow.openfinance.adapter.outbound.persistence

import com.finflow.openfinance.application.port.outbound.OpenFinanceConsentRepository
import com.finflow.openfinance.domain.ConsentStatus
import com.finflow.openfinance.domain.OpenFinanceConsent
import org.springframework.stereotype.Repository

@Repository
class JpaOpenFinanceConsentRepository(
    private val delegate: SpringDataOpenFinanceConsentRepository,
) : OpenFinanceConsentRepository {
    override fun save(value: OpenFinanceConsent): OpenFinanceConsent = delegate.save(value.toEntity()).toDomain()

    override fun findByUserIdAndProviderIgnoreCaseAndExternalConsentId(
        userId: Int,
        provider: String,
        externalConsentId: String,
    ): OpenFinanceConsent? = delegate.findByUserIdAndProviderIgnoreCaseAndExternalConsentId(userId, provider, externalConsentId)?.toDomain()

    override fun findAllByUserId(userId: Int): List<OpenFinanceConsent> = delegate.findAllByUserId(userId).map { it.toDomain() }

    override fun findAllByUserIdAndStatus(
        userId: Int,
        status: ConsentStatus,
    ): List<OpenFinanceConsent> =
        delegate.findAllByUserIdAndStatus(userId, status).map {
            it.toDomain()
        }
}
