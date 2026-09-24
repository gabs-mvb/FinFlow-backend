package com.finflow.shared.domain

class OnboardingRequiredException :
    ApiException(FailureReason.FORBIDDEN, "ONBOARDING_REQUIRED", "Conclua a configuração inicial para continuar")
