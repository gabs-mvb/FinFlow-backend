package com.finflow.planning.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek

fun nextIncomeDate(
    asOf: LocalDate,
    payDay: Int,
    schedule: com.finflow.onboarding.domain.IncomeSchedule = com.finflow.onboarding.domain.IncomeSchedule.DAY_OF_MONTH,
): LocalDate {
    require(payDay in 1..31) { "Dia de recebimento inválido" }
    val currentMonth = YearMonth.from(asOf)
    if (schedule == com.finflow.onboarding.domain.IncomeSchedule.VARIABLE) return asOf.plusMonths(1)
    if (schedule == com.finflow.onboarding.domain.IncomeSchedule.LAST_BUSINESS_DAY) {
        fun lastWeekday(month: YearMonth): LocalDate {
            var day = month.atEndOfMonth()
            while (day.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) day = day.minusDays(1)
            return day
        }
        val current = lastWeekday(currentMonth)
        return if (current > asOf) current else lastWeekday(currentMonth.plusMonths(1))
    }
    val currentDate = currentMonth.atDay(minOf(payDay, currentMonth.lengthOfMonth()))
    if (currentDate > asOf) return currentDate

    val nextMonth = currentMonth.plusMonths(1)
    return nextMonth.atDay(minOf(payDay, nextMonth.lengthOfMonth()))
}
