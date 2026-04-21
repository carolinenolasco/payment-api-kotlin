package com.empresa.payments.domain.model

enum class PaymentStatus {
    PENDING,     // Criando, aguardando autorização
    AUTHORIZED,  // Autorizado pela operadora
    CAPTURED,    // Valor capturado/ confirmado
    REFUNDED,    // Estorno realizado
    VOIDED       // Cancelado antes da captura
}
