package com.empresa.payments.domain.port.out

import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TransactionRepositoryPort {
    suspend fun save(transaction: Transaction): Transaction
    suspend fun findById(id: UUID): Transaction?
    fun findAll(): Flow<Transaction>
    fun findByPaymentId(paymentId: UUID): Flow<Transaction>
}