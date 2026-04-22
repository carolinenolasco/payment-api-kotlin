package com.empresa.payments.infrastructure.persistence.repository

import com.empresa.payments.domain.model.Transaction
import com.empresa.payments.domain.port.out.TransactionRepositoryPort
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionRepositoryAdapter(
    private val r2dbc: R2dbcTransactionRepository
) : TransactionRepositoryPort {

    override suspend fun save(transaction: Transaction): Transaction =
        r2dbc.save(transaction)

    override suspend fun findById(id: UUID): Transaction? =
        r2dbc.findById(id)

    override fun findAll(): Flow<Transaction> =
        r2dbc.findAll()

    override fun findByPaymentId(paymentId: UUID): Flow<Transaction> =
        r2dbc.findByPaymentId(paymentId)
}