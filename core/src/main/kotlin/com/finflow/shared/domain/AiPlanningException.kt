package com.finflow.shared.domain

class AiPlanningException(
    message: String,
    code: String = "AI_UNAVAILABLE",
) : ApiException(FailureReason.SERVICE_UNAVAILABLE, code, message)
