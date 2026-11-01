package com.empresa.payments

import com.empresa.payments.infrastructure.web.dto.PaymentResponse
import com.empresa.payments.infrastructure.web.dto.TransactionResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

// PostgreSQLContainer é genérico em Java; esta subclasse evita o "<*>" no Kotlin.
class KPostgresContainer : PostgreSQLContainer<KPostgresContainer>("postgres:15-alpine")

/**
 * Teste de ponta a ponta: aplicação completa + PostgreSQL real (container temporário).
 * Pega problemas que os testes com mocks não pegam, como o mapeamento de leitura do R2DBC.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentFlowIntegrationTest {

    companion object {
        // Sobe antes dos testes e é descartado no final.
        @Container
        @JvmField
        val postgres = KPostgresContainer()

        // Aponta o Spring para o banco do container (porta aleatória), no lugar do application.yml.
        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
        }
    }

    @Autowired
    lateinit var client: WebTestClient

    // ── Funções de apoio ────────────────────────────

    private fun criarPagamento(): UUID =
        client.post().uri("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("amount" to 150.00, "currency" to "BRL", "method" to "CREDIT_CARD"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(PaymentResponse::class.java)
            .returnResult()
            .responseBody!!
            .id!!

    private fun executar(id: UUID, operacao: String) =
        client.post().uri("/payment/$id/$operacao").exchange()

    // ── Testes ──────────────────────────────────────

    @Test
    fun `fluxo completo - cria, autoriza, captura e estorna`() {
        val id = criarPagamento()

        executar(id, "authorize")
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("AUTHORIZED")
        executar(id, "capture")
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("CAPTURED")
        executar(id, "refund")
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("REFUNDED")

        // Leitura do banco: o pagamento volta com o histórico completo, em ordem
        client.get().uri("/payment/$id").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("REFUNDED")
            .jsonPath("$.amount").isEqualTo(150.0)
            .jsonPath("$.transactions.length()").isEqualTo(3)
            .jsonPath("$.transactions[0].type").isEqualTo("AUTHORIZATION")
            .jsonPath("$.transactions[1].type").isEqualTo("CAPTURE")
            .jsonPath("$.transactions[2].type").isEqualTo("REFUND")
    }

    @Test
    fun `void - cancela pagamento autorizado e bloqueia estorno depois`() {
        val id = criarPagamento()

        executar(id, "authorize").expectStatus().isOk
        executar(id, "void")
            .expectStatus().isOk
            .expectBody().jsonPath("$.status").isEqualTo("VOIDED")
        executar(id, "refund").expectStatus().isEqualTo(422)
    }

    @Test
    fun `capture sem authorize - retorna 422 e o pagamento continua PENDING sem transacoes`() {
        val id = criarPagamento()

        executar(id, "capture").expectStatus().isEqualTo(422)

        client.get().uri("/payment/$id").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING")
            .jsonPath("$.transactions.length()").isEqualTo(0)
    }

    @Test
    fun `GET payment id transactions - lista so as transacoes daquele pagamento`() {
        val id = criarPagamento()
        executar(id, "authorize").expectStatus().isOk
        executar(id, "capture").expectStatus().isOk

        val outro = criarPagamento()
        executar(outro, "authorize").expectStatus().isOk

        client.get().uri("/payment/$id/transactions").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].paymentId").isEqualTo(id.toString())
            .jsonPath("$[1].paymentId").isEqualTo(id.toString())
    }

    @Test
    fun `GET transactions id - retorna a transacao gravada`() {
        val id = criarPagamento()
        executar(id, "authorize").expectStatus().isOk

        val transacao = client.get().uri("/payment/$id/transactions").exchange()
            .expectStatus().isOk
            .expectBodyList(TransactionResponse::class.java)
            .returnResult()
            .responseBody!!
            .single()

        client.get().uri("/transactions/${transacao.id}").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.type").isEqualTo("AUTHORIZATION")
            .jsonPath("$.paymentId").isEqualTo(id.toString())
    }

    @Test
    fun `GET payment - a lista contem o pagamento criado`() {
        val id = criarPagamento()

        val ids = client.get().uri("/payment").exchange()
            .expectStatus().isOk
            .expectBodyList(PaymentResponse::class.java)
            .returnResult()
            .responseBody!!
            .map { it.id }

        assertTrue(id in ids)
    }

    @Test
    fun `ids inexistentes - retornam 404`() {
        val inexistente = UUID.randomUUID()

        client.get().uri("/payment/$inexistente").exchange().expectStatus().isNotFound
        client.get().uri("/transactions/$inexistente").exchange().expectStatus().isNotFound
        executar(inexistente, "authorize").expectStatus().isNotFound
    }
}
