package com.finflow.authentication.application.model

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
) {
    init {
        require(name.isNotBlank()) { "name não pode estar vazio" }
        require(name.length in 0..100) { "Tamanho inválido para name" }
        require(email.isNotBlank()) { "email não pode estar vazio" }
        require(email.length in 0..150) { "Tamanho inválido para email" }
        require(password.isNotBlank()) { "password não pode estar vazio" }
        require(password.length in 0..72) { "Tamanho inválido para password" }
        require(email.contains("@") && !email.startsWith("@") && !email.endsWith("@")) { "E-mail inválido" }
    }
}
