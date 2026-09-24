package com.finflow.openfinance.domain

enum class ConsentStatus {
    AWAITING_AUTHORIZATION,
    ACTIVE,
    EXPIRED,
    REVOKED,
    REJECTED,
    FAILED,
}
