package com.empresa.payments.domain.service

import com.empresa.payments.domain.exception.InvalidPaymentStatusException
import com.empresa.payments.domain.exception.PaymentNotFoundException
import com.empresa.payments.domain.exception.TransactionNotFoundException
import com.empresa.payments.domain.model.CreatePaymentRequest
import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.model.PaymentStatus
import com.empresa.payments.domain.model.Transaction
import com.empresa.payments.domain.model.TransactionType
import com.empresa.payments.domain.port.`in`.PaymentUseCase
import com.empresa.payments.domain.port.out.PaymentRepositoryPort
import com.empresa.payments.domain.port.out.TransactionRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val paymentRepo: PaymentRepositoryPort,
    private val transactionRepo: TransactionRepositoryPort
) : PaymentUseCase {

    // ────────────────────────────────────────────
    // COMANDOS
    // ────────────────────────────────────────────

    // CRIAR PAGAMENTO
    override suspend fun createPayment(request: CreatePaymentRequest): Payment {
        val payment = Payment(
            amount = request.amount,
            currency = request.currency,
            method = request.method,
            status = PaymentStatus.PENDING,
            details = request.details
        )
        return paymentRepo.save(payment)
    }

    // AUTORIZAR (PENDING → AUTHORIZED)
    // @Transactional: se salvar o Payment falhar, a Transaction também é desfeita
    @Transactional
    override suspend fun authorizePayment(id: UUID): Payment =
        transition(id, PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, TransactionType.AUTHORIZATION)

    // CAPTURAR (AUTHORIZED → CAPTURED)
    @Transactional
    override suspend fun capturePayment(id: UUID): Payment =
        transition(id, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, TransactionType.CAPTURE)

    // ESTORNAR (CAPTURED → REFUNDED)
    @Transactional
    override suspend fun refundPayment(id: UUID): Payment =
        transition(id, PaymentStatus.CAPTURED, PaymentStatus.REFUNDED, TransactionType.REFUND)

    // CANCELAR (AUTHORIZED → VOIDED)
    @Transactional
    override suspend fun voidPayment(id: UUID): Payment =
        transition(id, PaymentStatus.AUTHORIZED, PaymentStatus.VOIDED, TransactionType.VOID)

    /**
     * Regra comum das 4 operações: valida o status atual, registra a Transaction
     * e move o Payment para o novo status.
     */
    private suspend fun transition(
        id: UUID,
        from: PaymentStatus,
        to: PaymentStatus,
        type: TransactionType
    ): Payment {
        val payment = paymentRepo.findById(id)
            ?: throw PaymentNotFoundException(id)

        if (payment.status != from)
            throw InvalidPaymentStatusException(payment.status, from)

        transactionRepo.save(
            Transaction(
                type = type,
                amount = payment.amount,
                status = "SUCCESS",
                paymentId = id
            )
        )

        return paymentRepo.save(payment.copy(status = to)).withTransactions()
    }

    // ────────────────────────────────────────────
    // QUERIES
    // ────────────────────────────────────────────

    override fun findAllPayments(): Flow<Payment> = paymentRepo.findAll()

    override suspend fun findPaymentById(id: UUID): Payment =
        (paymentRepo.findById(id) ?: throw PaymentNotFoundException(id)).withTransactions()

    override fun findAllTransactions(): Flow<Transaction> = transactionRepo.findAll()

    override suspend fun findTransactionById(id: UUID): Transaction =
        transactionRepo.findById(id) ?: throw TransactionNotFoundException(id)

    override fun findTransactionsByPaymentId(paymentId: UUID): Flow<Transaction> =
        transactionRepo.findByPaymentId(paymentId)

    // O campo Payment.transactions é @Transient (não é coluna) — carregamos aqui, em ordem cronológica.
    private suspend fun Payment.withTransactions(): Payment {
        val paymentId = requireNotNull(id) { "Payment ainda não persistido" }
        return copy(transactions = transactionRepo.findByPaymentId(paymentId).toList().sortedBy { it.timestamp })
    }
}
