package com.finflow.shared.domain

import com.finflow.authentication.domain.User

enum class FailureReason { NOT_FOUND, UNPROCESSABLE_ENTITY, FORBIDDEN, CONFLICT, BAD_REQUEST, SERVICE_UNAVAILABLE }

class AccountDeactivatedException : ApiException(FailureReason.FORBIDDEN, "REQUEST_REJECTED", "User account is deactivated")

class RegistrationRejectedException(
    message: String,
) : ApiException(FailureReason.BAD_REQUEST, "REQUEST_REJECTED", message)
