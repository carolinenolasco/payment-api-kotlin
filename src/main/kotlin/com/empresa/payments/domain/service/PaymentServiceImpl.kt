package com.empresa.payments.domain.service

import com.empresa.payments.domain.exception.*
import com.empresa.payments.domain.model.*
import com.empresa.payments.domain.port.`in`.PaymentUseCase
import com.empresa.payments.domain.port.out.PaymentRepositoryPort
import com.empresa.payments.domain.port.out.TransactionRepositoryPort
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val paymentRepo: PaymentRepositoryPort,
    private val transactionRepo: TransactionRepositoryPort
) : PaymentUseCase {

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

    // AUTORIZAR PAGAMENTO (PENDING → AUTHORIZED)
    override suspend fun authorizePayment(id: UUID): Payment {
        val payment = paymentRepo.findById(id)
            ?: throw PaymentNotFoundException(id)
        if (payment.status != PaymentStatus.PENDING)
            throw InvalidPaymentStatusException(payment.status, PaymentStatus.PENDING)
        val transaction = Transaction(
            type = TransactionType.AUTHORIZATION,
            amount = payment.amount,
            status = "SUCCESS",
            paymentId = id
        )
        transactionRepo.save(transaction)
        return paymentRepo.save(payment.copy(status = PaymentStatus.AUTHORIZED))
    }

    // CAPTURAR PAGAMENTO (AUTHORIZED → CAPTURED)
    override suspend fun capturePayment(id: UUID): Payment {
        val payment = paymentRepo.findById(id)
        ?: throw PaymentNotFoundException(id)

        if (payment.status != PaymentStatus.AUTHORIZED)
        throw InvalidPaymentStatusException(payment.status, PaymentStatus.AUTHORIZED)

        val transaction = Transaction(
        type = TransactionType.CAPTURE,
        amount = payment.amount,
        status = "SUCCESS",
        paymentId = id
    )
        transactionRepo.save(transaction)
        return paymentRepo.save(payment.copy(status = PaymentStatus.CAPTURED))
    }

    // ESTORNAR PAGAMENTO (CAPTURED → REFUNDED)
    override suspend fun refundPayment(id: UUID): Payment {
        val payment = paymentRepo.findById(id)
        ?: throw PaymentNotFoundException(id)

        if (payment.status != PaymentStatus.CAPTURED)
            throw InvalidPaymentStatusException(payment.status, PaymentStatus.CAPTURED)

        val transaction = Transaction(
            type = TransactionType.REFUND,
            amount = payment.amount,
            status = "SUCCESS",
            paymentId = id
        )

        transactionRepo.save(transaction)
        return paymentRepo.save(payment.copy(status = PaymentStatus.REFUNDED))
    }

    // CANCELAR PAGAMENTO (AUTHORIZED → VOIDED)
    override suspend fun voidPayment(id: UUID): Payment {
        val payment = paymentRepo.findById(id)
            ?: throw PaymentNotFoundException(id)
        if (payment.status != PaymentStatus.AUTHORIZED)
            throw InvalidPaymentStatusException(payment.status,
                PaymentStatus.AUTHORIZED)
        val transaction = Transaction(
            type
            = TransactionType.VOID,
            amount
            = payment.amount,
            status
            = "SUCCESS",
            paymentId = id
        )
        transactionRepo.save(transaction)
        return paymentRepo.save(payment.copy(status = PaymentStatus.VOIDED))
    }

    // QUERIES
    override fun findAllPayments(): Flow<Payment> = paymentRepo.findAll()
    override suspend fun findPaymentById(id: UUID): Payment =
        paymentRepo.findById(id) ?: throw PaymentNotFoundException(id)
    override fun findAllTransactions(): Flow<Transaction> = transactionRepo.findAll()
    override suspend fun findTransactionById(id: UUID): Transaction =
        transactionRepo.findById(id)
            ?: throw RuntimeException("Transação não encontrada: $id")
    override fun findTransactionsByPaymentId(paymentId: UUID): Flow<Transaction> =
        transactionRepo.findByPaymentId(paymentId)
}
