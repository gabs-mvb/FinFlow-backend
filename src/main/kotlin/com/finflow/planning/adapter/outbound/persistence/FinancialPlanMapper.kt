package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.domain.FinancialPlan
import com.finflow.planning.domain.PlanDetails
import com.finflow.planning.domain.PlannedAllocation
import com.finflow.portfolio.domain.AssetClass
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal

private val detailsJson = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

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
        warnings = decodeWarnings(warnings),
        allocations = decodeAllocations(allocationPlan),
        generatedAt = generatedAt,
        revision = revision,
        updatedAt = updatedAt,
        details = planDetails?.let { detailsJson.readValue(it, PlanDetails::class.java) } ?: PlanDetails(),
        totalBalanceSnapshot = totalBalanceSnapshot,
        reserveBalanceSnapshot = reserveBalanceSnapshot,
        reserveTargetSnapshot = reserveTargetSnapshot,
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
        warnings = detailsJson.writeValueAsString(warnings),
        allocationPlan = allocations.joinToString("|") { "${it.assetClass.name}:${it.amount}" },
        generatedAt = generatedAt,
        revision = revision,
        updatedAt = updatedAt,
        planDetails = detailsJson.writeValueAsString(details),
        totalBalanceSnapshot = totalBalanceSnapshot,
        reserveBalanceSnapshot = reserveBalanceSnapshot,
        reserveTargetSnapshot = reserveTargetSnapshot,
    )

private fun decodeWarnings(value: String): List<String> =
    if (value.startsWith("[")) {
        runCatching { detailsJson.readValue(value, Array<String>::class.java).toList() }
            .getOrElse { value.split('|').filter { it.isNotBlank() } }
    } else {
        value.split('|').filter { it.isNotBlank() }
    }

private fun decodeAllocations(value: String): List<PlannedAllocation> =
    value.split('|').filter { it.isNotBlank() }.mapNotNull { item ->
        val parts = item.split(':', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        runCatching { PlannedAllocation(AssetClass.valueOf(parts[0]), BigDecimal(parts[1])) }.getOrNull()
    }
