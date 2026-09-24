package com.finflow.shared.application.port.outbound

import com.finflow.authentication.domain.User

interface CurrentUser {
    fun user(): User

    fun id(): Int = user().id
}
