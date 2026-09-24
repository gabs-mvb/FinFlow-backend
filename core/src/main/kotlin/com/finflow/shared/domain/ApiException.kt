package com.finflow.shared.domain

sealed class ApiException(
    val reason: FailureReason,
    val code: String,
    override val message: String,
) : RuntimeException(message)
