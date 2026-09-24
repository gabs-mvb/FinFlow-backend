package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.domain.FinancialPlan
import com.finflow.planning.domain.PlannedAllocation
import com.finflow.portfolio.domain.AssetClass
import java.math.BigDecimal

internal fun FinancialPlanEntity.toDomain(): FinancialPlan =
    FinancialPlan(
        userId = userId,
        id = id,
        asOf = asOf,
        nextIncomeDate = nextIncomeDate,
        currency = currency,
        operatingBalance = operatingBalance,
        committedObligations = committedObligations,
        remainingVariableBudget = remainingVariableBudget,
        minimumCashBuffer = minimumCashBuffer,
        debtPaymentRecommendation = debtPaymentRecommendation,
        reserveContribution = reserveContribution,
        investmentContribution = investmentContribution,
        freeRealBalance = freeRealBalance,
        projectedShortfall = projectedShortfall,
        dailySpendingLimit = dailySpendingLimit,
        warnings = warnings.split('|').filter { it.isNotBlank() },
        allocations = decodeAllocations(allocationPlan),
        generatedAt = generatedAt,
    )

internal fun FinancialPlan.toEntity(): FinancialPlanEntity =
    FinancialPlanEntity(
        userId = userId,
        id = id,
        asOf = asOf,
        nextIncomeDate = nextIncomeDate,
        currency = currency,
        operatingBalance = operatingBalance,
        committedObligations = committedObligations,
        remainingVariableBudget = remainingVariableBudget,
        minimumCashBuffer = minimumCashBuffer,
        debtPaymentRecommendation = debtPaymentRecommendation,
        reserveContribution = reserveContribution,
        investmentContribution = investmentContribution,
        freeRealBalance = freeRealBalance,
        projectedShortfall = projectedShortfall,
        dailySpendingLimit = dailySpendingLimit,
        warnings = warnings.joinToString("|") { it.replace("|", "/") },
        allocationPlan = allocations.joinToString("|") { "${it.assetClass.name}:${it.amount}" },
        generatedAt = generatedAt,
    )

private fun decodeAllocations(value: String): List<PlannedAllocation> =
    value.split('|').filter { it.isNotBlank() }.mapNotNull { item ->
        val parts = item.split(':', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        runCatching { PlannedAllocation(AssetClass.valueOf(parts[0]), BigDecimal(parts[1])) }.getOrNull()
    }
