package com.empresa.payments.domain.service

import com.empresa.payments.domain.exception.InvalidPaymentStatusException
import com.empresa.payments.domain.exception.PaymentNotFoundException
import com.empresa.payments.domain.exception.TransactionNotFoundException
import com.empresa.payments.domain.model.CreatePaymentRequest
import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.model.PaymentStatus
import com.empresa.payments.domain.model.Transaction
import com.empresa.payments.domain.model.TransactionType
import com.empresa.payments.domain.port.out.PaymentRepositoryPort
import com.empresa.payments.domain.port.out.TransactionRepositoryPort
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class PaymentServiceImplTest {

	private val paymentRepo = mockk<PaymentRepositoryPort>()
	private val transactionRepo = mockk<TransactionRepositoryPort>()
	private val service = PaymentServiceImpl(paymentRepo, transactionRepo)

	private fun payment(status: PaymentStatus = PaymentStatus.PENDING) = Payment(
		id = UUID.randomUUID(),
		amount = BigDecimal("100.00"),
		currency = "BRL",
		method = "PIX",
		status = status
	)

	/** Stubs comuns: save devolve o que recebeu e captura a Transaction gravada. */
	private fun stubSaves(captured: CapturingSlot<Transaction> = slot()) {
		coEvery { transactionRepo.save(capture(captured)) } answers { firstArg() }
		coEvery { paymentRepo.save(any()) } answers { firstArg() }
		every { transactionRepo.findByPaymentId(any()) } returns emptyFlow()
	}

	// ── CRIAR ───────────────────────────────────────

	@Test
	fun `deve criar pagamento com status PENDING`() = runTest {
		coEvery { paymentRepo.save(any()) } answers { firstArg() }

		val result = service.createPayment(
			CreatePaymentRequest(BigDecimal("100.00"), "BRL", "CREDIT_CARD")
		)

		assertEquals(PaymentStatus.PENDING, result.status)
		assertEquals(BigDecimal("100.00"), result.amount)
		assertEquals("BRL", result.currency)
		coVerify(exactly = 1) { paymentRepo.save(any()) }
	}

	// ── TRANSIÇÕES VÁLIDAS ──────────────────────────

	private suspend fun assertTransition(
		from: PaymentStatus,
		to: PaymentStatus,
		type: TransactionType,
		action: suspend (UUID) -> Payment
	) {
		val payment = payment(from)
		val captured = slot<Transaction>()
		coEvery { paymentRepo.findById(payment.id!!) } returns payment
		stubSaves(captured)

		val result = action(payment.id!!)

		assertEquals(to, result.status)
		assertEquals(type, captured.captured.type)
		assertEquals(payment.amount, captured.captured.amount)
		assertEquals(payment.id, captured.captured.paymentId)
		coVerify(exactly = 1) { transactionRepo.save(any()) }
		coVerify(exactly = 1) { paymentRepo.save(match { it.status == to }) }
	}

	@Test
	fun `authorize - PENDING vira AUTHORIZED e registra AUTHORIZATION`() = runTest {
		assertTransition(
			PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, TransactionType.AUTHORIZATION
		) { service.authorizePayment(it) }
	}

	@Test
	fun `capture - AUTHORIZED vira CAPTURED e registra CAPTURE`() = runTest {
		assertTransition(
			PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, TransactionType.CAPTURE
		) { service.capturePayment(it) }
	}

	@Test
	fun `refund - CAPTURED vira REFUNDED e registra REFUND`() = runTest {
		assertTransition(
			PaymentStatus.CAPTURED, PaymentStatus.REFUNDED, TransactionType.REFUND
		) { service.refundPayment(it) }
	}

	@Test
	fun `void - AUTHORIZED vira VOIDED e registra VOID`() = runTest {
		assertTransition(
			PaymentStatus.AUTHORIZED, PaymentStatus.VOIDED, TransactionType.VOID
		) { service.voidPayment(it) }
	}

	// ── TRANSIÇÕES INVÁLIDAS ────────────────────────

	/** Uma operação do service e o único status de origem em que ela é permitida. */
	private class Operation(
		val requiredStatus: PaymentStatus,
		val run: suspend (UUID) -> Payment
	)

	@Test
	fun `cada operacao rejeita qualquer status diferente do esperado e nao grava nada`() = runTest {
		val operations = listOf(
			Operation(PaymentStatus.PENDING) { service.authorizePayment(it) },
			Operation(PaymentStatus.AUTHORIZED) { service.capturePayment(it) },
			Operation(PaymentStatus.CAPTURED) { service.refundPayment(it) },
			Operation(PaymentStatus.AUTHORIZED) { service.voidPayment(it) }
		)

		for (operation in operations) {
			for (wrongStatus in PaymentStatus.entries.filter { it != operation.requiredStatus }) {
				val payment = payment(wrongStatus)
				coEvery { paymentRepo.findById(payment.id!!) } returns payment

				assertThrows<InvalidPaymentStatusException> { operation.run(payment.id!!) }
			}
		}

		coVerify(exactly = 0) { transactionRepo.save(any()) }
		coVerify(exactly = 0) { paymentRepo.save(any()) }
	}

	@Test
	fun `deve lancar PaymentNotFoundException se o pagamento nao existir`() = runTest {
		coEvery { paymentRepo.findById(any()) } returns null

		assertThrows<PaymentNotFoundException> { service.authorizePayment(UUID.randomUUID()) }
		assertThrows<PaymentNotFoundException> { service.capturePayment(UUID.randomUUID()) }
		assertThrows<PaymentNotFoundException> { service.refundPayment(UUID.randomUUID()) }
		assertThrows<PaymentNotFoundException> { service.voidPayment(UUID.randomUUID()) }
		assertThrows<PaymentNotFoundException> { service.findPaymentById(UUID.randomUUID()) }
	}

	// ── QUERIES ─────────────────────────────────────

	@Test
	fun `findPaymentById deve carregar as transactions em ordem cronologica`() = runTest {
		val payment = payment(PaymentStatus.CAPTURED)
		val now = LocalDateTime.now()
		val captureTx = Transaction(
			type = TransactionType.CAPTURE, amount = payment.amount,
			timestamp = now, paymentId = payment.id!!
		)
		val authorizationTx = Transaction(
			type = TransactionType.AUTHORIZATION, amount = payment.amount,
			timestamp = now.minusMinutes(5), paymentId = payment.id!!
		)
		coEvery { paymentRepo.findById(payment.id!!) } returns payment
		// devolvidas fora de ordem de propósito
		every { transactionRepo.findByPaymentId(payment.id!!) } returns flowOf(captureTx, authorizationTx)

		val result = service.findPaymentById(payment.id!!)

		assertEquals(
			listOf(TransactionType.AUTHORIZATION, TransactionType.CAPTURE),
			result.transactions.map { it.type }
		)
	}

	@Test
	fun `findTransactionById deve lancar TransactionNotFoundException se nao existir`() = runTest {
		coEvery { transactionRepo.findById(any()) } returns null

		assertThrows<TransactionNotFoundException> { service.findTransactionById(UUID.randomUUID()) }
	}
}
