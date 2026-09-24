package com.finflow.planning.application.port.outbound

import com.finflow.planning.application.model.PlanRevision
import java.util.UUID

interface PlanRevisionRepository {
    fun save(revision: PlanRevision)

    fun findAllByPlanIdAndUserId(
        planId: UUID,
        userId: Int,
    ): List<PlanRevision>
}
