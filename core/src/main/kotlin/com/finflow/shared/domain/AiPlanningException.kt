package com.finflow.shared.domain

class AiPlanningException(
    message: String,
    code: String = "AI_UNAVAILABLE",
    val providerStatus: Int? = null,
    val providerRequestId: String? = null,
) : ApiException(FailureReason.SERVICE_UNAVAILABLE, code, message)
