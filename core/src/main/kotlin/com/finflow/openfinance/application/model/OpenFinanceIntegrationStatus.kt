package com.finflow.openfinance.application.model

data class OpenFinanceIntegrationStatus(
    val configuredProvider: String,
    val liveSynchronizationAvailable: Boolean,
    val mode: String,
    val warning: String?,
)
