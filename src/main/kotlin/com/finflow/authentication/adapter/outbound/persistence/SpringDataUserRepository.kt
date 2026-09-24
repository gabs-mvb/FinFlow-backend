package com.finflow.authentication.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SpringDataUserRepository : JpaRepository<UserEntity, Int> {
    fun findByEmail(email: String): Optional<UserEntity>

    fun existsByEmail(email: String): Boolean
}
