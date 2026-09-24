package com.finflow.profile.adapter.outbound.persistence

import com.finflow.profile.application.port.outbound.FinancialProfileRepository
import com.finflow.profile.domain.FinancialProfile
import org.springframework.stereotype.Repository

@Repository
class JpaFinancialProfileRepository(
    private val delegate: SpringDataFinancialProfileRepository,
) : FinancialProfileRepository {
    override fun save(value: FinancialProfile): FinancialProfile = delegate.save(value.toEntity()).toDomain()

    override fun findByUserId(userId: Int): FinancialProfile? = delegate.findByUserId(userId)?.toDomain()
}
