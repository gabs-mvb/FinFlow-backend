package com.finflow.report.application.model

import com.finflow.shared.application.model.MoneyOutput
import com.finflow.transaction.domain.TransactionCategory
import java.math.BigDecimal

data class CategoryExpense(
    val category: TransactionCategory,
    val amount: MoneyOutput,
    val percentageOfExpenses: BigDecimal,
)
