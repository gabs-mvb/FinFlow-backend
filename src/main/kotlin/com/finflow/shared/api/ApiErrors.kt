package com.finflow.shared.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

sealed class ApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
) : RuntimeException(message)

class ResourceNotFoundException(message: String) :
    ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message)

class BusinessRuleException(message: String, code: String = "BUSINESS_RULE_VIOLATION") :
    ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message)

class ConflictException(message: String, code: String = "CONFLICT") :
    ApiException(HttpStatus.CONFLICT, code, message)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(exception.status, exception.message)
        problem.title = exception.code
        problem.setProperty("code", exception.code)
        problem.setProperty("path", request.requestURI)
        return ResponseEntity.status(exception.status).body(problem)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val errors = exception.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "valor inválido")
        }
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "A requisição contém campos inválidos",
        )
        problem.title = "VALIDATION_ERROR"
        problem.setProperty("code", "VALIDATION_ERROR")
        problem.setProperty("path", request.requestURI)
        problem.setProperty("errors", errors)
        return ResponseEntity.badRequest().body(problem)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.message ?: "Argumento inválido",
        )
        problem.title = "INVALID_ARGUMENT"
        problem.setProperty("code", "INVALID_ARGUMENT")
        problem.setProperty("path", request.requestURI)
        return ResponseEntity.badRequest().body(problem)
    }
}

