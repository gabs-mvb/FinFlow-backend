package com.finflow.profile.adapter.outbound.persistence

import com.finflow.profile.domain.FinancialProfile

internal fun FinancialProfileEntity.toDomain(): FinancialProfile =
    FinancialProfile(
        userId = userId,
        id = id,
        currency = currency,
        monthlyIncome = monthlyIncome,
        payDay = payDay,
        essentialMonthlyExpenses = essentialMonthlyExpenses,
        variableMonthlyBudget = variableMonthlyBudget,
        minimumCashBuffer = minimumCashBuffer,
        emergencyTargetMonths = emergencyTargetMonths,
        reserveContributionRate = reserveContributionRate,
        investmentContributionRate = investmentContributionRate,
        riskProfile = riskProfile,
        autopilotMode = autopilotMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun FinancialProfile.toEntity(): FinancialProfileEntity =
    FinancialProfileEntity(
        userId = userId,
        id = id,
        currency = currency,
        monthlyIncome = monthlyIncome,
        payDay = payDay,
        essentialMonthlyExpenses = essentialMonthlyExpenses,
        variableMonthlyBudget = variableMonthlyBudget,
        minimumCashBuffer = minimumCashBuffer,
        emergencyTargetMonths = emergencyTargetMonths,
        reserveContributionRate = reserveContributionRate,
        investmentContributionRate = investmentContributionRate,
        riskProfile = riskProfile,
        autopilotMode = autopilotMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
