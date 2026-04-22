package com.empresa.payments.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal          // ✅ este import precisa estar aqui
import java.time.LocalDateTime
import java.util.UUID

@Table("transactions")
data class Transaction(
    @Id
    val id: UUID? = null,
    val type: TransactionType,
    val amount: BigDecimal,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: String = "SUCCESS",

    val details: String? = null,

    @Column("payment_id")
    val paymentId: UUID
)