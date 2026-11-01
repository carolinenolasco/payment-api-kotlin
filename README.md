# payment-api-kotlin

API REST reativa de pagamentos com **Kotlin, Spring Boot 3, WebFlux, R2DBC e PostgreSQL**, organizada em Arquitetura Hexagonal (portas e adaptadores).

## Ciclo de vida do pagamento

| Status atual | Ação        | Novo status  |
|--------------|-------------|--------------|
| `PENDING`    | `authorize` | `AUTHORIZED` |
| `AUTHORIZED` | `capture`   | `CAPTURED`   |
| `CAPTURED`   | `refund`    | `REFUNDED`   |
| `AUTHORIZED` | `void`      | `VOIDED`     |

Cada ação registra uma `Transaction` (`AUTHORIZATION`, `CAPTURE`, `REFUND`, `VOID`) e só é permitida a partir do status indicado; fora disso a API responde `422`.

## Como rodar

Pré-requisitos: JDK 17+ e Docker.

```bash
# 1. Sobe o PostgreSQL local
docker-compose up -d

# 2. Roda a API (o schema.sql é aplicado automaticamente na subida)
./gradlew bootRun
```

A API sobe em `http://localhost:8080`.
O PostgreSQL do `docker-compose` usa a porta **5433** no seu computador (e não a 5432), para não conflitar com um Postgres já instalado.

## Endpoints

| Método | Endpoint                          | Descrição                         | Sucesso |
|--------|-----------------------------------|-----------------------------------|---------|
| POST   | `/payment`                        | Cria pagamento (`PENDING`)        | 201     |
| POST   | `/payment/{id}/authorize`         | Autoriza                          | 200     |
| POST   | `/payment/{id}/capture`           | Captura                           | 200     |
| POST   | `/payment/{id}/refund`            | Estorna                           | 200     |
| POST   | `/payment/{id}/void`              | Cancela                           | 200     |
| GET    | `/payment`                        | Lista pagamentos                  | 200     |
| GET    | `/payment/{id}`                   | Busca pagamento (com transações)  | 200     |
| GET    | `/transactions`                   | Lista transações                  | 200     |
| GET    | `/transactions/{id}`              | Busca transação                   | 200     |
| GET    | `/payment/{id}/transactions`      | Transações de um pagamento        | 200     |

### Erros

| Status | Quando                                                       |
|--------|--------------------------------------------------------------|
| 400    | Validação do body (`campos` indica quais) ou UUID inválido   |
| 404    | Pagamento/transação não encontrado                           |
| 422    | Transição de status não permitida                            |
| 500    | Erro inesperado (detalhes só no log)                         |

### Exemplo

```bash
# Criar
curl -X POST http://localhost:8080/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 150.00, "currency": "BRL", "method": "CREDIT_CARD"}'

# Autorizar e capturar (troque {ID} pelo id retornado)
curl -X POST http://localhost:8080/payment/{ID}/authorize
curl -X POST http://localhost:8080/payment/{ID}/capture

# Ver o pagamento com o histórico de transações
curl http://localhost:8080/payment/{ID}
```

## Testes

```bash
./gradlew test
```

- `PaymentServiceImplTest` — regras de negócio (MockK, sem banco).
- `PaymentControllerTest` — camada HTTP com `@WebFluxTest` + `WebTestClient` (sem banco).
- `PaymentFlowIntegrationTest` — fluxo completo com PostgreSQL real via Testcontainers; **precisa do Docker rodando**, mas não do `docker-compose`.

## Estrutura

```
domain/                 # núcleo: sem dependência de web ou banco
├── exception/
├── model/              # Payment, Transaction, enums
├── port/in/            # PaymentUseCase (o que a API faz)
├── port/out/           # repositórios (o que o domínio precisa)
└── service/            # PaymentServiceImpl
infrastructure/
├── persistence/repository/   # adapters R2DBC
└── web/                      # controller, DTOs, GlobalExceptionHandler
```