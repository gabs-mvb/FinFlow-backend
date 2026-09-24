package com.finflow.audit.application.port.inbound

interface AuditUseCases {
    fun record(
        action: String,
        resourceType: String,
        resourceId: Any? = null,
        details: String? = null,
    ): Unit
}
