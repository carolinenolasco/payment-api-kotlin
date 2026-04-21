package com.empresa.payments.infrastructure.persistence.repository

import com.empresa.payments.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface R2dbcTransactionRepository : CoroutineCrudRepository<Transaction, UUID> {

    fun findByPaymentId(paymentId: UUID): Flow<Transaction>
}
