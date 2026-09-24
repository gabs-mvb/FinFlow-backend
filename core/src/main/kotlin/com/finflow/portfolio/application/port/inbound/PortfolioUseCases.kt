package com.finflow.portfolio.application.port.inbound

import com.finflow.portfolio.application.model.ContributionAllocation
import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.model.ReplacePortfolioRequest
import com.finflow.shared.domain.Money

interface PortfolioUseCases {
    fun replace(request: ReplacePortfolioRequest): PortfolioResponse

    fun get(): PortfolioResponse

    fun allocateContribution(contribution: Money): List<ContributionAllocation>
}
