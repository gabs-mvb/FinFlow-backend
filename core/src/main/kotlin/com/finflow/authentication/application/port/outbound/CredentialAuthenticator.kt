package com.finflow.authentication.application.port.outbound

fun interface CredentialAuthenticator {
    fun authenticate(
        email: String,
        password: String,
    ): String
}
