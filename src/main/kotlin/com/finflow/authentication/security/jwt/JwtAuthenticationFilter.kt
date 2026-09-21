package com.finflow.authentication.security.jwt

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var username: String? = null
        var jwtToken: String? = null

        val requestTokenHeader = request.getHeader("Authorization")

        if (!requestTokenHeader.isNullOrBlank() && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7)

            try {
                username = jwtTokenProvider.getUsernameFromToken(jwtToken)
            } catch (e: ExpiredJwtException) {
                LoggerFactory.getLogger(JwtAuthenticationFilter::class.java).info(
                    "[AUTHENTICATION FAILURE] - Token expired, user: {} - {}",
                    e.claims.subject,
                    e.message
                )
                response.status = HttpServletResponse.SC_UNAUTHORIZED
            } catch (e: Exception) {
                LoggerFactory.getLogger(JwtAuthenticationFilter::class.java).error(
                    "[AUTHENTICATION FAILURE] - Invalid token: {}", 
                    e.message
                )
                response.status = HttpServletResponse.SC_UNAUTHORIZED
            }
        }

        if (!username.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            addUsernameInContext(request, username, jwtToken)
        }

        filterChain.doFilter(request, response)
    }

    private fun addUsernameInContext(request: HttpServletRequest, username: String, jwtToken: String?) {
        if (jwtToken.isNullOrBlank()) return

        val userDetails = userDetailsService.loadUserByUsername(username)

        if (jwtTokenProvider.validateToken(jwtToken, userDetails.username)) {
            val authenticationToken = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authenticationToken
        }
    }
}
