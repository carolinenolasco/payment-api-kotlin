package com.empresa.payments.domain.port.out

import com.empresa.payments.domain.model.Payment
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface PaymentRepositoryPort {
    suspend fun save(payment: Payment): Payment
    suspend fun findById(id: UUID): Payment?
    fun findAll(): Flow<Payment>
}