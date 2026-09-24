package com.finflow.profile.application.port.outbound

import com.finflow.profile.domain.FinancialProfile

interface FinancialProfileRepository {
    fun save(value: FinancialProfile): FinancialProfile

    fun findByUserId(userId: Int): FinancialProfile?
}
