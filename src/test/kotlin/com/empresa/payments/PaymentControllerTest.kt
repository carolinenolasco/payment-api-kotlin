package com.empresa.payments.infrastructure.web

import com.empresa.payments.domain.exception.InvalidPaymentStatusException
import com.empresa.payments.domain.exception.PaymentNotFoundException
import com.empresa.payments.domain.exception.TransactionNotFoundException
import com.empresa.payments.domain.model.Payment
import com.empresa.payments.domain.model.PaymentStatus
import com.empresa.payments.domain.model.Transaction
import com.empresa.payments.domain.model.TransactionType
import com.empresa.payments.domain.port.`in`.PaymentUseCase
import com.empresa.payments.infrastructure.web.controller.PaymentController
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.util.UUID

// @WebFluxTest sobe só a camada web (sem banco). O PaymentUseCase é um mock.
@WebFluxTest(PaymentController::class)
class PaymentControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @MockkBean
    lateinit var useCase: PaymentUseCase

    private val id = UUID.randomUUID()

    private fun payment(status: PaymentStatus = PaymentStatus.PENDING) = Payment(
        id = id,
        amount = BigDecimal("100.00"),
        currency = "BRL",
        method = "CREDIT_CARD",
        status = status
    )

    private fun transaction(type: TransactionType = TransactionType.AUTHORIZATION) = Transaction(
        id = UUID.randomUUID(),
        type = type,
        amount = BigDecimal("100.00"),
        paymentId = id
    )

    // ── POST /payment ───────────────────────────────

    @Test
    fun `POST payment - cria com status PENDING e retorna 201`() {
        coEvery { useCase.createPayment(any()) } returns payment()

        client.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("amount" to 100.00, "currency" to "BRL", "method" to "CREDIT_CARD"))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING")
            .jsonPath("$.id").isEqualTo(id.toString())
            .jsonPath("$.amount").isEqualTo(100.0)
    }

    @Test
    fun `POST payment - valor zero retorna 400 com o campo invalido`() {
        client.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("amount" to 0, "currency" to "BRL", "method" to "PIX"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.campos.amount").isNotEmpty
    }

    @Test
    fun `POST payment - moeda com tamanho errado retorna 400`() {
        client.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("amount" to 10, "currency" to "BR", "method" to "PIX"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.campos.currency").isNotEmpty
    }

    @Test
    fun `POST payment - campo obrigatorio ausente retorna 400`() {
        client.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("currency" to "BRL", "method" to "PIX"))
            .exchange()
            .expectStatus().isBadRequest
    }

    // ── Transições ──────────────────────────────────

    @Test
    fun `POST authorize - retorna 200 AUTHORIZED`() {
        coEvery { useCase.authorizePayment(id) } returns
                payment(PaymentStatus.AUTHORIZED).copy(transactions = listOf(transaction()))

        client.post().uri("/payment/$id/authorize")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("AUTHORIZED")
            .jsonPath("$.transactions[0].type").isEqualTo("AUTHORIZATION")
    }

    @Test
    fun `POST capture - retorna 200 CAPTURED`() {
        coEvery { useCase.capturePayment(id) } returns payment(PaymentStatus.CAPTURED)

        client.post().uri("/payment/$id/capture")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("CAPTURED")
    }

    @Test
    fun `POST refund - retorna 200 REFUNDED`() {
        coEvery { useCase.refundPayment(id) } returns payment(PaymentStatus.REFUNDED)

        client.post().uri("/payment/$id/refund")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("REFUNDED")
    }

    @Test
    fun `POST void - retorna 200 VOIDED`() {
        coEvery { useCase.voidPayment(id) } returns payment(PaymentStatus.VOIDED)

        client.post().uri("/payment/$id/void")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("VOIDED")
    }

    @Test
    fun `POST capture em status errado - retorna 422`() {
        coEvery { useCase.capturePayment(id) } throws
                InvalidPaymentStatusException(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED)

        client.post().uri("/payment/$id/capture")
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody().jsonPath("$.status").isEqualTo(422)
    }

    @Test
    fun `POST authorize em pagamento inexistente - retorna 404`() {
        coEvery { useCase.authorizePayment(id) } throws PaymentNotFoundException(id)

        client.post().uri("/payment/$id/authorize")
            .exchange()
            .expectStatus().isNotFound
    }

    // ── GET /payment ────────────────────────────────

    @Test
    fun `GET payment - retorna a lista`() {
        every { useCase.findAllPayments() } returns flowOf(payment(), payment(PaymentStatus.CAPTURED))

        client.get().uri("/payment")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
    }

    @Test
    fun `GET payment por id - retorna o pagamento`() {
        coEvery { useCase.findPaymentById(id) } returns payment()

        client.get().uri("/payment/$id")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.id").isEqualTo(id.toString())
    }

    @Test
    fun `GET payment id inexistente - retorna 404`() {
        coEvery { useCase.findPaymentById(any()) } throws PaymentNotFoundException(id)

        client.get().uri("/payment/00000000-0000-0000-0000-000000000000")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `GET payment com id que nao e UUID - retorna 400`() {
        client.get().uri("/payment/abc")
            .exchange()
            .expectStatus().isBadRequest
    }

    // ── GET /transactions ───────────────────────────

    @Test
    fun `GET transactions - retorna a lista`() {
        every { useCase.findAllTransactions() } returns flowOf(transaction(), transaction(TransactionType.CAPTURE))

        client.get().uri("/transactions")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.length()").isEqualTo(2)
    }

    @Test
    fun `GET transactions id inexistente - retorna 404`() {
        coEvery { useCase.findTransactionById(any()) } throws TransactionNotFoundException(id)

        client.get().uri("/transactions/$id")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `GET payment id transactions - retorna as transacoes do pagamento`() {
        every { useCase.findTransactionsByPaymentId(id) } returns flowOf(transaction())

        client.get().uri("/payment/$id/transactions")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].paymentId").isEqualTo(id.toString())
            .jsonPath("$[0].type").isEqualTo("AUTHORIZATION")
    }
}
