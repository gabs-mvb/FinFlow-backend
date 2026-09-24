package com.finflow.obligation.adapter.outbound.persistence

import com.finflow.obligation.domain.ObligationStatus
import com.finflow.obligation.domain.ObligationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "obligations")
class ObligationEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 160)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 40)
    var obligationType: ObligationType = ObligationType.OTHER,
    @Column(nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3)
    var currency: String = "BRL",
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var status: ObligationStatus = ObligationStatus.PENDING,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
