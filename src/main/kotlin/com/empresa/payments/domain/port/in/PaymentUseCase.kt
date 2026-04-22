package com.empresa.payments.domain.port.`in`

import com.empresa.payments.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface PaymentUseCase {

    suspend fun createPayment(request: CreatePaymentRequest): Payment
    suspend fun authorizePayment(id: UUID): Payment
    suspend fun capturePayment(id: UUID): Payment
    suspend fun refundPayment(id: UUID): Payment
    suspend fun voidPayment(id: UUID): Payment

    fun findAllPayments(): Flow<Payment>
    suspend fun findPaymentById(id: UUID): Payment
    fun findAllTransactions(): Flow<Transaction>
    suspend fun findTransactionById(id: UUID): Transaction
    fun findTransactionsByPaymentId(paymentId: UUID): Flow<Transaction>
}