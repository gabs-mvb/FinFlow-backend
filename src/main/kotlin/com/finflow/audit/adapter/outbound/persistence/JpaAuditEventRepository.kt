package com.finflow.audit.adapter.outbound.persistence

import com.finflow.audit.application.port.outbound.AuditEventRepository
import com.finflow.audit.domain.AuditEvent
import org.springframework.stereotype.Repository

@Repository
class JpaAuditEventRepository(
    private val delegate: SpringDataAuditEventRepository,
) : AuditEventRepository {
    override fun save(value: AuditEvent): AuditEvent = delegate.save(value.toEntity()).toDomain()
}
