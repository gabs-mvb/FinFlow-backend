package com.finflow.obligation.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class Obligation(
    val userId: Int? = null,
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val obligationType: ObligationType = ObligationType.OTHER,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BRL",
    val dueDate: LocalDate = LocalDate.now(),
    val recurring: Boolean = false,
    val dueDay: Int? = null,
    val status: ObligationStatus = ObligationStatus.PENDING,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    init {
        require(!recurring || dueDay in 1..31) { "Dia de vencimento recorrente deve estar entre 1 e 31" }
        require(recurring || dueDay == null) { "Dia de vencimento exige recorrência" }
    }

    fun nextDueDate(): LocalDate {
        require(recurring)
        return dateIn(YearMonth.from(dueDate).plusMonths(1))
    }

    fun occurrencesUntil(endExclusive: LocalDate): List<LocalDate> {
        if (status != ObligationStatus.PENDING || dueDate >= endExclusive) return emptyList()
        if (!recurring) return listOf(dueDate)
        val result = mutableListOf<LocalDate>()
        var month = YearMonth.from(dueDate)
        while (month.atDay(1) < endExclusive) {
            val date = dateIn(month)
            if (date >= dueDate && date < endExclusive) result += date
            month = month.plusMonths(1)
        }
        return result
    }

    private fun dateIn(month: YearMonth): LocalDate = month.atDay(minOf(requireNotNull(dueDay), month.lengthOfMonth()))

    companion object {
        fun firstDueDate(
            from: LocalDate,
            dueDay: Int,
        ): LocalDate {
            require(dueDay in 1..31) { "Dia de vencimento recorrente deve estar entre 1 e 31" }
            val month = YearMonth.from(from)
            val current = month.atDay(minOf(dueDay, month.lengthOfMonth()))
            return if (current >= from) current else month.plusMonths(1).atDay(minOf(dueDay, month.plusMonths(1).lengthOfMonth()))
        }
    }
}
