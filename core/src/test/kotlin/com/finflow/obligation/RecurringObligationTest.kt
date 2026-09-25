package com.finflow.obligation

import com.finflow.obligation.domain.Obligation
import com.finflow.obligation.domain.ObligationStatus
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecurringObligationTest {
    @Test
    fun `day 31 falls on last calendar day and returns to 31 when available`() {
        val obligation =
            Obligation(
                dueDate = LocalDate.parse("2027-01-31"),
                recurring = true,
                dueDay = 31,
            )
        assertEquals(LocalDate.parse("2027-02-28"), obligation.nextDueDate())
        assertEquals(
            listOf("2027-01-31", "2027-02-28", "2027-03-31").map(LocalDate::parse),
            obligation.occurrencesUntil(LocalDate.parse("2027-04-01")),
        )
    }

    @Test
    fun `one time and paid commitments do not recur`() {
        val oneTime = Obligation(dueDate = LocalDate.parse("2027-01-10"))
        assertEquals(listOf(LocalDate.parse("2027-01-10")), oneTime.occurrencesUntil(LocalDate.parse("2027-04-01")))
        assertEquals(emptyList(), oneTime.copy(status = ObligationStatus.PAID).occurrencesUntil(LocalDate.parse("2027-04-01")))
        assertFailsWith<IllegalArgumentException> { Obligation(recurring = true, dueDay = 32) }
    }
}
