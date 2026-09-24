package com.finflow.audit.application.port.outbound

import com.finflow.audit.domain.AuditEvent

interface AuditEventRepository {
    fun save(value: AuditEvent): AuditEvent
}
