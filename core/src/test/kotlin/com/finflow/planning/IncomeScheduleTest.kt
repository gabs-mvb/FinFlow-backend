package com.finflow.planning

import com.finflow.onboarding.domain.IncomeSchedule
import com.finflow.planning.domain.nextIncomeDate
import java.time.LocalDate
import kotlin.test.*

class IncomeScheduleTest {
    @Test fun `last weekday is estimated and rolls to next month after payday`() {
        assertEquals(LocalDate.parse("2026-10-30"), nextIncomeDate(LocalDate.parse("2026-10-01"), 31, IncomeSchedule.LAST_BUSINESS_DAY))
        assertEquals(LocalDate.parse("2026-11-30"), nextIncomeDate(LocalDate.parse("2026-10-30"), 31, IncomeSchedule.LAST_BUSINESS_DAY))
    }
    @Test fun `variable income uses explicit forecast horizon instead of inventing a payday`() {
        assertEquals(LocalDate.parse("2026-11-30"), nextIncomeDate(LocalDate.parse("2026-10-31"), 31, IncomeSchedule.VARIABLE))
    }
}
