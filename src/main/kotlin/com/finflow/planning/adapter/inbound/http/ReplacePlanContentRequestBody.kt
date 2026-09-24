package com.finflow.planning.adapter.inbound.http

import com.finflow.planning.application.model.ReplacePlanContentRequest
import com.finflow.planning.domain.PlanContent
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ReplacePlanContentRequestBody(
    @field:NotNull @field:Min(0) val expectedRevision: Int?,
    @field:NotNull val content: PlanContent?,
) {
    fun toCommand() = ReplacePlanContentRequest(requireNotNull(expectedRevision), requireNotNull(content))
}
