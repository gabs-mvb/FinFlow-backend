package com.finflow.authentication.application.port.outbound

fun interface PasswordHasher {
    fun encode(password: String): String
}
