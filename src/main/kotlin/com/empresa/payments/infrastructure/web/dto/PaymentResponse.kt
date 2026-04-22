package com.empresa.payments.infrastructure.web.dto

import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PaymentResponse(
    val id: UUID?,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
    val method: String,
    val timestamp: LocalDateTime,
    val details: String?,
    val transactions: List<TransactionResponse> = emptyList()
) {
    companion object {
        fun from(payment: Payment) = PaymentResponse(
            id = payment.id,
            amount = payment.amount,
            currency = payment.currency,
            status = payment.status.name,
            method = payment.method,
            timestamp = payment.timestamp,
            details = payment.details,
            transactions = payment.transactions.map { TransactionResponse.from(it) }
        )
    }
}

data class TransactionResponse(
    val id: UUID?,
    val type: String,
    val amount: BigDecimal,
    val timestamp: LocalDateTime,
    val status: String,
    val paymentId: UUID
) {
    companion object {
        fun from(t: Transaction) = TransactionResponse(
            id = t.id,
            type = t.type.name,
            amount = t.amount,
            timestamp = t.timestamp,
            status = t.status,
            paymentId = t.paymentId
        )
    }
}