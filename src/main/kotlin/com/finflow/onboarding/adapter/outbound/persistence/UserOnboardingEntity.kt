package com.finflow.onboarding.adapter.outbound.persistence

import com.finflow.onboarding.domain.*
import com.finflow.onboarding.application.port.outbound.UserOnboardingRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime

@Entity
@Table(name = "user_onboarding")
class UserOnboardingEntity(
    @Id @Column(name = "user_id") var userId: Int = 0,
    @Enumerated(EnumType.STRING) var status: OnboardingProgress = OnboardingProgress.NOT_STARTED,
    @Enumerated(EnumType.STRING) @Column(name = "current_step") var currentStep: OnboardingStage = OnboardingStage.WELCOME,
    @Column(columnDefinition = "text", nullable = false) var payload: String = "{}",
    @Column(name = "started_at") var startedAt: OffsetDateTime? = null,
    @Column(name = "completed_at") var completedAt: OffsetDateTime? = null,
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

interface SpringDataUserOnboardingRepository : JpaRepository<UserOnboardingEntity, Int> {
    fun findByUserId(userId: Int): UserOnboardingEntity?
}

@Repository
class JpaUserOnboardingRepository(private val delegate: SpringDataUserOnboardingRepository, private val json: ObjectMapper) : UserOnboardingRepository {
    override fun findByUserId(userId: Int): UserOnboarding? = delegate.findByUserId(userId)?.let {
        UserOnboarding(it.userId, it.status, it.currentStep, json.readValue(it.payload, OnboardingData::class.java), it.startedAt, it.completedAt, it.updatedAt)
    }
    override fun save(value: UserOnboarding): UserOnboarding {
        delegate.save(UserOnboardingEntity(value.userId, value.status, value.currentStep, json.writeValueAsString(value.data), value.startedAt, value.completedAt, value.updatedAt))
        return value
    }
}
