package com.finflow.shared.domain

class ResourceNotFoundException(
    message: String,
) : ApiException(FailureReason.NOT_FOUND, "RESOURCE_NOT_FOUND", message)
