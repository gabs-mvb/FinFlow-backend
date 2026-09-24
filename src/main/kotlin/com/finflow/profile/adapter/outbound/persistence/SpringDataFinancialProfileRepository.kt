package com.finflow.profile.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFinancialProfileRepository : JpaRepository<FinancialProfileEntity, UUID> {
    fun findByUserId(userId: Int): FinancialProfileEntity?
}
