package com.finflow.account.application.port.inbound

import com.finflow.account.application.model.CreateAccountRequest
import com.finflow.account.application.model.FinancialAccountResponse
import com.finflow.account.application.model.UpdateAccountBalanceRequest
import com.finflow.account.application.model.UpdateAccountRequest
import com.finflow.account.domain.FinancialAccount
import java.util.UUID

interface FinancialAccountUseCases {
    fun create(request: CreateAccountRequest): FinancialAccountResponse

    fun update(
        id: UUID,
        request: UpdateAccountRequest,
    ): FinancialAccountResponse

    fun updateBalance(
        id: UUID,
        request: UpdateAccountBalanceRequest,
    ): FinancialAccountResponse

    fun list(): List<FinancialAccountResponse>

    fun getRequired(id: UUID): FinancialAccount
}
