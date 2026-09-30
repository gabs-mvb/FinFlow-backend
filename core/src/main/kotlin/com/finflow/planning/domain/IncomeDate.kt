package com.finflow.planning.domain

import java.time.LocalDate
import java.time.YearMonth

fun nextIncomeDate(
    asOf: LocalDate,
    payDay: Int,
): LocalDate {
    require(payDay in 1..31) { "Dia de recebimento inválido" }
    val currentMonth = YearMonth.from(asOf)
    val currentDate = currentMonth.atDay(minOf(payDay, currentMonth.lengthOfMonth()))
    if (currentDate > asOf) return currentDate

    val nextMonth = currentMonth.plusMonths(1)
    return nextMonth.atDay(minOf(payDay, nextMonth.lengthOfMonth()))
}
