package com.finflow.report.adapter.inbound.http

import com.finflow.report.application.model.MonthlyFinancialReport
import com.finflow.report.application.port.inbound.MonthlyReportUseCases
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/v1/reports")
@Validated
class MonthlyReportController(
    private val service: MonthlyReportUseCases,
) {
    @GetMapping("/monthly")
    fun monthly(
        @RequestParam @Min(2000) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int,
    ): MonthlyFinancialReport = service.generate(YearMonth.of(year, month))
}
