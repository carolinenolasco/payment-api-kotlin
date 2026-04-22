package com.empresa.payments.infrastructure.web

import com.empresa.payments.domain.exception.InvalidPaymentStatusException
import com.empresa.payments.domain.exception.PaymentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: PaymentNotFoundException) =
        mapOf("erro" to (ex.message ?: "Não encontrado"), "status" to 404)

    @ExceptionHandler(InvalidPaymentStatusException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidStatus(ex: InvalidPaymentStatusException) =
        mapOf("erro" to (ex.message ?: "Status inválido"), "status" to 422)

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception) =
        mapOf("erro" to "Erro interno: ${ex.message}", "status" to 500)
}