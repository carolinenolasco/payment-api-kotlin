package com.empresa.payments.domain.exception

import com.empresa.payments.domain.model.PaymentStatus
import java.util.UUID

class PaymentNotFoundException(id: UUID) :
    RuntimeException("Pagamento não encontrado com ID: $id")

class TransactionNotFoundException(id: UUID) :
    RuntimeException("Transação não encontrada com ID: $id")

class InvalidPaymentStatusException(
    current: PaymentStatus,
    required: PaymentStatus
) : RuntimeException(
    "Status inválido: $current. Status necessário: $required"
)
