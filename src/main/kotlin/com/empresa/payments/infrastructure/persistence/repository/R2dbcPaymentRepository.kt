package com.empresa.payments.infrastructure.persistence.repository

import com.empresa.payments.domain.model.Payment
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface R2dbcPaymentRepository : CoroutineCrudRepository<Payment, UUID>
