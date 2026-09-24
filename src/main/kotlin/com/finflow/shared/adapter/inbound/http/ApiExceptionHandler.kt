package com.finflow.shared.adapter.inbound.http

import com.finflow.shared.domain.ApiException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> = problem(HttpStatus.valueOf(exception.reason.name), exception.code, exception.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val errors =
            exception.bindingResult.fieldErrors.associate { error ->
                error.field to (error.defaultMessage ?: "valor inválido")
            }
        return problem(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "A requisição contém campos inválidos",
            request,
            mapOf("errors" to errors),
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARGUMENT",
            exception.message ?: "Argumento inválido",
            request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.debug("Corpo de requisição inválido em {}", request.requestURI, exception)
        return problem(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST_BODY",
            "O corpo da requisição não contém um JSON válido",
            request,
        )
    }

    @ExceptionHandler(
        MissingRequestHeaderException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleRequestBinding(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST",
            requestBindingMessage(exception),
            request,
        )

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataConflict(
        exception: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.warn("Conflito de integridade em {}", request.requestURI, exception)
        return problem(
            HttpStatus.CONFLICT,
            "DATA_CONFLICT",
            "A operação conflita com um registro existente",
            request,
        )
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException::class)
    fun handleAuthentication(
        exception: org.springframework.security.core.AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Credenciais inválidas ou sessão expirada",
            request,
        )

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException::class)
    fun handleStatus(
        exception: org.springframework.web.server.ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        problem(
            HttpStatus.valueOf(exception.statusCode.value()),
            "REQUEST_REJECTED",
            exception.reason ?: "Solicitação recusada",
            request,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        logger.error("Falha inesperada em {}", request.requestURI, exception)
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Não foi possível concluir a operação",
            request,
        )
    }

    private fun requestBindingMessage(exception: Exception): String =
        when (exception) {
            is MissingRequestHeaderException -> "O header ${exception.headerName} é obrigatório"
            is MissingServletRequestParameterException -> "O parâmetro ${exception.parameterName} é obrigatório"
            is MethodArgumentTypeMismatchException -> "O valor informado para ${exception.name} é inválido"
            else -> "A requisição contém um valor inválido"
        }

    private fun problem(
        status: HttpStatus,
        code: String,
        detail: String,
        request: HttpServletRequest,
        properties: Map<String, Any> = emptyMap(),
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = code
        problem.setProperty("code", code)
        problem.setProperty("path", request.requestURI)
        properties.forEach(problem::setProperty)
        return ResponseEntity.status(status).body(problem)
    }
}
