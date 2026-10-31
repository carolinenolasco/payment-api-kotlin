package com.empresa.payments.infrastructure.web.controller

import com.empresa.payments.domain.model.CreatePaymentRequest
import com.empresa.payments.domain.port.`in`.PaymentUseCase
import com.empresa.payments.infrastructure.web.dto.PaymentRequest
import com.empresa.payments.infrastructure.web.dto.PaymentResponse
import com.empresa.payments.infrastructure.web.dto.TransactionResponse
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class PaymentController(private val useCase: PaymentUseCase) {

    // ── POST /payment
    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createPayment(
        @RequestBody @Valid request: PaymentRequest
    ): PaymentResponse {
        val domainRequest = CreatePaymentRequest(
            amount = request.amount,
            currency = request.currency,
            method = request.method,
            details = request.details
        )
        return PaymentResponse.from(useCase.createPayment(domainRequest))
    }

    // ── POST /payment/{id}/authorize
    @PostMapping("/payment/{id}/authorize")
    suspend fun authorize(@PathVariable id: UUID): PaymentResponse =
        PaymentResponse.from(useCase.authorizePayment(id))

    // ── POST /payment/{id}/capture
    @PostMapping("/payment/{id}/capture")
    suspend fun capture(@PathVariable id: UUID): PaymentResponse =
        PaymentResponse.from(useCase.capturePayment(id))

    // ── POST /payment/{id}/refund
    @PostMapping("/payment/{id}/refund")
    suspend fun refund(@PathVariable id: UUID): PaymentResponse =
        PaymentResponse.from(useCase.refundPayment(id))

    // ── POST /payment/{id}/void
    @PostMapping("/payment/{id}/void")
    suspend fun void(@PathVariable id: UUID): PaymentResponse =
        PaymentResponse.from(useCase.voidPayment(id))

    // ── GET /payment  (lista todos os pagamentos)
    @GetMapping("/payment")
    fun getAllPayments(): Flow<PaymentResponse> =
        useCase.findAllPayments().map { PaymentResponse.from(it) }

    // ── GET /payment/{id}
    @GetMapping("/payment/{id}")
    suspend fun getPaymentById(@PathVariable id: UUID): PaymentResponse =
        PaymentResponse.from(useCase.findPaymentById(id))

    // ── GET /transactions  (todas as transações)
    @GetMapping("/transactions")
    fun getAllTransactions(): Flow<TransactionResponse> =
        useCase.findAllTransactions().map { TransactionResponse.from(it) }

    // ── GET /transactions/{id}
    @GetMapping("/transactions/{id}")
    suspend fun getTransactionById(@PathVariable id: UUID): TransactionResponse =
        TransactionResponse.from(useCase.findTransactionById(id))

    // ── GET /payment/{paymentId}/transactions
    @GetMapping("/payment/{paymentId}/transactions")
    fun getTransactionsByPayment(@PathVariable paymentId: UUID): Flow<TransactionResponse> =
        useCase.findTransactionsByPaymentId(paymentId)
            .map { TransactionResponse.from(it) }
}
