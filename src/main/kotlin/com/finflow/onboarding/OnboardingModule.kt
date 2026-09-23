package com.finflow.onboarding

import com.finflow.authentication.domain.UserRepository
import com.finflow.profile.FinancialProfileService
import com.finflow.profile.UpsertFinancialProfileRequest
import com.finflow.shared.api.OnboardingRequiredException
import com.finflow.shared.security.CurrentUser
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

data class OnboardingStatus(val completed: Boolean)

@Service
class OnboardingService(
    private val currentUser: CurrentUser,
    private val profiles: FinancialProfileService,
    private val users: UserRepository,
    private val entityManager: EntityManager,
) {
    fun status(): OnboardingStatus = OnboardingStatus(currentUser.user().onboardingCompleted)

    @Transactional
    fun complete(request: UpsertFinancialProfileRequest): OnboardingStatus {
        val user = currentUser.user()
        // Serialize repeated submissions and commit profile + completion together.
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE)
        if (!user.onboardingCompleted) {
            profiles.upsert(request)
            user.onboardingCompleted = true
            users.save(user)
        }
        return OnboardingStatus(true)
    }
}

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController(private val service: OnboardingService) {
    @GetMapping
    fun status(): OnboardingStatus = service.status()

    @PostMapping("/complete")
    fun complete(@Valid @RequestBody request: UpsertFinancialProfileRequest): OnboardingStatus =
        service.complete(request)
}
