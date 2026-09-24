package com.finflow.planning.application.port.outbound

import com.finflow.planning.application.model.PlanProposal
import com.finflow.planning.application.model.PlanningEvidence

fun interface PlanAdvisor {
    fun suggest(
        evidence: PlanningEvidence,
        preferences: String,
    ): PlanProposal
}
