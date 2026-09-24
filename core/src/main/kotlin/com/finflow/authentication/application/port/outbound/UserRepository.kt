package com.finflow.authentication.application.port.outbound

import com.finflow.authentication.domain.User
import java.util.Optional

interface UserRepository {
    fun save(value: User): User

    fun lockById(id: Int): User

    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean
}
