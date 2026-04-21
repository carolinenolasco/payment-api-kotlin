package com.empresa.payments.infrastructure.persistence.repository

import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.port.out.PaymentRepositoryPort
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentRepositoryAdapter(
    private val r2dbc: R2dbcPaymentRepository
) : PaymentRepositoryPort {

    override suspend fun save(payment: Payment) : Payment =
        r2dbc.save(payment)

    override suspend fun findById(id: UUID): Payment? =
        r2dbc.findById(id)

    override fun findAll(): Flow<Payment> =
        r2dbc.findAll()
}
