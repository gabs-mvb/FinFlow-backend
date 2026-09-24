package com.finflow.portfolio.application.port.outbound

import com.finflow.portfolio.domain.AllocationTarget

interface AllocationTargetRepository {
    fun save(value: AllocationTarget): AllocationTarget

    fun saveAll(values: List<AllocationTarget>): List<AllocationTarget>

    fun findAllByUserId(userId: Int): List<AllocationTarget>

    fun deleteAllByUserId(userId: Int)
}
