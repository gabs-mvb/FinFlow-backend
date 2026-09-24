package com.finflow.authentication.adapter.outbound.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.function.Function

@Component
class JwtTokenProvider {
    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var jwtTokenValidity: Long = 86400000

    fun generateToken(authentication: Authentication): String =
        Jwts
            .builder()
            .setSubject(authentication.name)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + jwtTokenValidity))
            .signWith(parseSecret())
            .compact()

    fun getUsernameFromToken(token: String): String = getClaimFromToken(token, Claims::getSubject)

    fun getExpirationDateFromToken(token: String): Date = getClaimFromToken(token, Claims::getExpiration)

    fun <T> getClaimFromToken(
        token: String,
        claimsResolver: Function<Claims, T>,
    ): T {
        val claims = getAllClaimsFromToken(token)
        return claimsResolver.apply(claims)
    }

    fun validateToken(
        token: String,
        username: String,
    ): Boolean {
        val tokenUsername = getUsernameFromToken(token)
        return tokenUsername == username && !isTokenExpired(token)
    }

    fun isTokenExpired(token: String): Boolean {
        val expirationDate = getExpirationDateFromToken(token)
        return expirationDate.before(Date(System.currentTimeMillis()))
    }

    private fun getAllClaimsFromToken(token: String): Claims =
        Jwts
            .parserBuilder()
            .setSigningKey(parseSecret())
            .build()
            .parseClaimsJws(token)
            .body

    private fun parseSecret() = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
}
