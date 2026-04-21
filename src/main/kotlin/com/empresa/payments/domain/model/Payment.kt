package com.empresa.payments.domain.model

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Table("payment")
data class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),

    val amount: BigDecimal,
    val currency: String,

    var status: PaymentStatus = PaymentStatus.PENDING,

    val method: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: String? = null,

    @Transient
    val transactions: List<PulsarProperties.Transaction> = emptyList(),
)