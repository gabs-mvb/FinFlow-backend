package com.finflow.profile.application.port.inbound

import com.finflow.profile.application.model.FinancialProfileResponse
import com.finflow.profile.application.model.UpsertFinancialProfileRequest
import com.finflow.profile.domain.FinancialProfile

interface FinancialProfileUseCases {
    fun upsert(request: UpsertFinancialProfileRequest): FinancialProfileResponse

    fun getRequired(): FinancialProfile

    fun get(): FinancialProfileResponse
}
