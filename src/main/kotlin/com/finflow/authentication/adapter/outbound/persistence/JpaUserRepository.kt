package com.finflow.authentication.adapter.outbound.persistence

import com.finflow.authentication.application.port.outbound.UserRepository
import com.finflow.authentication.domain.User
import com.finflow.shared.domain.ResourceNotFoundException
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class JpaUserRepository(
    private val delegate: SpringDataUserRepository,
    private val entityManager: jakarta.persistence.EntityManager,
) : UserRepository {
    override fun save(value: User): User = delegate.save(value.toEntity()).toDomain()

    override fun findByEmail(email: String): Optional<User> = delegate.findByEmail(email).map { it.toDomain() }

    override fun existsByEmail(email: String): Boolean = delegate.existsByEmail(email)

    override fun lockById(id: Int): User {
        val entity =
            entityManager.find(UserEntity::class.java, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                ?: throw ResourceNotFoundException("Usuário não encontrado")
        entityManager.refresh(entity, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
        return entity.toDomain()
    }
}
