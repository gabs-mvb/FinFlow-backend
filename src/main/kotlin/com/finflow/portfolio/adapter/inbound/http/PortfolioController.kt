package com.finflow.portfolio.adapter.inbound.http

import com.finflow.portfolio.application.model.PortfolioResponse
import com.finflow.portfolio.application.port.inbound.PortfolioUseCases
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/portfolio")
class PortfolioController(
    private val service: PortfolioUseCases,
) {
    @PutMapping
    fun replace(
        @Valid @RequestBody request: ReplacePortfolioRequestBody,
    ): PortfolioResponse = service.replace(request.toCommand())

    @GetMapping
    fun get(): PortfolioResponse = service.get()
}
