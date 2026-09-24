package com.finflow.report.application.port.inbound

import com.finflow.report.application.model.MonthlyFinancialReport
import java.time.YearMonth

interface MonthlyReportUseCases {
    fun generate(period: YearMonth): MonthlyFinancialReport
}
