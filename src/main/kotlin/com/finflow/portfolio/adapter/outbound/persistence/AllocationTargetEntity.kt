package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.domain.AssetClass
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "allocation_targets")
class AllocationTargetEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 40)
    var assetClass: AssetClass = AssetClass.CASH,
    @Column(name = "target_percentage", nullable = false, precision = 7, scale = 4)
    var targetPercentage: BigDecimal = BigDecimal.ZERO,
    @Column(name = "minimum_percentage", nullable = false, precision = 7, scale = 4)
    var minimumPercentage: BigDecimal = BigDecimal.ZERO,
    @Column(name = "maximum_percentage", nullable = false, precision = 7, scale = 4)
    var maximumPercentage: BigDecimal = BigDecimal.ZERO,
)
