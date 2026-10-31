package com.empresa.payments.infrastructure.web

import com.empresa.payments.domain.exception.InvalidPaymentStatusException
import com.empresa.payments.domain.exception.PaymentNotFoundException
import com.empresa.payments.domain.exception.TransactionNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun body(status: Int, erro: String): Map<String, Any> =
        mapOf("erro" to erro, "status" to status)

    // 404 — pagamento ou transação inexistente
    @ExceptionHandler(PaymentNotFoundException::class, TransactionNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: RuntimeException): Map<String, Any> =
        body(404, ex.message ?: "Não encontrado")

    // 422 — transição de status não permitida (ex: capturar um pagamento PENDING)
    @ExceptionHandler(InvalidPaymentStatusException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidStatus(ex: InvalidPaymentStatusException): Map<String, Any> =
        body(422, ex.message ?: "Status inválido")

    // 400 — falha nas validações do @Valid (amount, currency, method)
    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: WebExchangeBindException): Map<String, Any> {
        val campos = ex.fieldErrors.associate { it.field to (it.defaultMessage ?: "inválido") }
        return body(400, "Dados inválidos") + ("campos" to campos)
    }

    // 400 — UUID mal formatado na URL, JSON quebrado, campo com tipo errado
    @ExceptionHandler(ServerWebInputException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadInput(ex: ServerWebInputException): Map<String, Any> {
        log.debug("Requisição inválida: {}", ex.message)
        return body(400, "Requisição inválida: verifique o formato dos parâmetros e do JSON")
    }

    // Mantém o status original de erros do próprio Spring (404 de rota, 405, 415...)
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(ex.statusCode)
            .body(body(ex.statusCode.value(), ex.reason ?: "Erro na requisição"))

    // 500 — qualquer outra coisa. Detalhe vai pro log, não pro cliente.
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception): Map<String, Any> {
        log.error("Erro inesperado", ex)
        return body(500, "Erro interno do servidor")
    }
}
