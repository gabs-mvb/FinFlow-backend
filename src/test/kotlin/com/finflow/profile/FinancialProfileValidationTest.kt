package com.finflow.profile

import com.finflow.profile.adapter.inbound.http.UpsertFinancialProfileRequestBody
import com.finflow.shared.adapter.inbound.http.MoneyInputBody
import jakarta.validation.Validation
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class FinancialProfileValidationTest {
    @Test
    fun `HTTP profile accepts pay days 1 through 31 and rejects values outside month`() {
        Validation.buildDefaultValidatorFactory().use { factory ->
            val money = MoneyInputBody(BigDecimal("100.00"))
            val profile = UpsertFinancialProfileRequestBody(money, 1, money, money, money)
            for (day in listOf(0, 1, 28, 29, 30, 31, 32)) {
                val violations = factory.validator.validate(profile.copy(payDay = day))
                assertEquals(day !in 1..31, violations.any { it.propertyPath.toString() == "payDay" }, "payDay=$day")
            }
        }
    }
}
