package com.finflow.portfolio.adapter.outbound.persistence

import com.finflow.portfolio.domain.AssetClass
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "portfolio_positions")
class PortfolioPositionEntity(
    @Column(name = "user_id", updatable = false)
    var userId: Int? = null,
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "asset_code", nullable = false, length = 48)
    var assetCode: String = "",
    @Column(name = "asset_name", nullable = false, length = 160)
    var assetName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 40)
    var assetClass: AssetClass = AssetClass.CASH,
    @Column(name = "current_value", nullable = false, precision = 19, scale = 2)
    var currentValue: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, length = 3) var currency: String = "BRL",
    @Column(name = "updated_at", nullable = false) var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
