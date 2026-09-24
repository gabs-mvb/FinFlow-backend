package com.finflow.shared.domain

class BusinessRuleException(
    message: String,
    code: String = "BUSINESS_RULE_VIOLATION",
) : ApiException(FailureReason.UNPROCESSABLE_ENTITY, code, message)
