package com.finflow.portfolio.application.port.inbound

import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.model.ReplacePortfolioRequest

interface PortfolioUseCases {
    fun replace(request: ReplacePortfolioRequest): PortfolioResponse

    fun get(): PortfolioResponse

}
