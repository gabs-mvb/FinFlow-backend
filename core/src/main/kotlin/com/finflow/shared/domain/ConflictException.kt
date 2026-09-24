package com.finflow.shared.domain

class ConflictException(
    message: String,
    code: String = "CONFLICT",
) : ApiException(FailureReason.CONFLICT, code, message)
