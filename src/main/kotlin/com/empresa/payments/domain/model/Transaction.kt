package com.empresa.payments.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
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

    // Detalhes extras em JSON (ex: código de autorização)
    val details: String? = null,

    // FK — referencia o Payment dono desta transação
    @Column("payment_id")
    val paymentId: UUID
)
