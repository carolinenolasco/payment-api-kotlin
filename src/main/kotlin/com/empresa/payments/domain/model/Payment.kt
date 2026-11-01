package com.empresa.payments.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Table("payments")
data class Payment(
    // null = ainda não persistido. O PostgreSQL gera o UUID (gen_random_uuid()).
    // Se o id já vier preenchido, o Spring Data R2DBC tenta fazer UPDATE em vez de INSERT.
    @Id
    val id: UUID? = null,
    val amount: BigDecimal,
    val currency: String,

    val status: PaymentStatus = PaymentStatus.PENDING,
    val method: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),

    // R2DBC não suporta Map<> — salvar como JSON string
    val details: String? = null,

    // @Transient = NÃO é coluna do banco. Carregado separado pelo service.
    @Transient
    val transactions: List<Transaction> = emptyList()
) {
    // Construtor usado pelo Spring Data ao LER do banco: só tem as colunas reais.
    // Sem ele, o Spring tenta preencher "transactions", que não é coluna, e falha.
    @PersistenceCreator
    constructor(
        id: UUID?,
        amount: BigDecimal,
        currency: String,
        status: PaymentStatus,
        method: String,
        timestamp: LocalDateTime,
        details: String?
    ) : this(id, amount, currency, status, method, timestamp, details, emptyList())
}
