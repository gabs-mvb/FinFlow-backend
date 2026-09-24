package com.finflow.planning.adapter.outbound.persistence

import com.finflow.planning.application.model.FinancialPlanResponse
import com.finflow.planning.application.model.PlanRevision
import com.finflow.planning.application.port.outbound.PlanRevisionRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "financial_plan_revisions", uniqueConstraints = [UniqueConstraint(columnNames = ["plan_id", "revision"])])
class PlanRevisionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "plan_id", nullable = false) var planId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: Int = 0,
    @Column(nullable = false) var revision: Int = 0,
    @Column(name = "captured_at", nullable = false) var capturedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(nullable = false, columnDefinition = "text") var snapshot: String = "",
)

interface SpringDataPlanRevisionRepository : JpaRepository<PlanRevisionEntity, UUID> {
    fun findAllByPlanIdAndUserIdOrderByRevisionDesc(
        planId: UUID,
        userId: Int,
    ): List<PlanRevisionEntity>
}

@Repository
class JpaPlanRevisionRepository(
    private val delegate: SpringDataPlanRevisionRepository,
    private val json: ObjectMapper,
) : PlanRevisionRepository {
    override fun save(revision: PlanRevision) {
        delegate.save(
            PlanRevisionEntity(
                planId = revision.planId,
                userId = revision.userId,
                revision = revision.revision,
                capturedAt = revision.capturedAt,
                snapshot = json.writeValueAsString(revision.plan),
            ),
        )
    }

    override fun findAllByPlanIdAndUserId(
        planId: UUID,
        userId: Int,
    ): List<PlanRevision> =
        delegate.findAllByPlanIdAndUserIdOrderByRevisionDesc(planId, userId).map {
            PlanRevision(it.planId, it.userId, it.revision, it.capturedAt, json.readValue(it.snapshot, FinancialPlanResponse::class.java))
        }
}
