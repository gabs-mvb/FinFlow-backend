package com.finflow.audit.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataAuditEventRepository : JpaRepository<AuditEventEntity, UUID>
