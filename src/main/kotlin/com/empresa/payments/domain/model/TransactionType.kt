package com.empresa.payments.domain.model

enum class TransactionType {
    AUTHORIZATION,  // Reserva do valor no cartão
    CAPTURE,        // Confirmacao da cobranca
    REFUND,         // Devolução ao cliente
    VOID            // Cancelando antes da captura
}
