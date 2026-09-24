package com.finflow.authentication.application.model

data class LoginRequestDto(
    val email: String,
    val password: String,
) {
    init {
        require(email.isNotBlank()) { "email não pode estar vazio" }
        require(email.length in 0..150) { "Tamanho inválido para email" }
        require(password.isNotBlank()) { "password não pode estar vazio" }
        require(password.length in 0..72) { "Tamanho inválido para password" }
    }
}
