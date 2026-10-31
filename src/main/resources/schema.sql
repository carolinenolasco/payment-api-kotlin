-- Habilitar extensão UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tabela de Pagamentos
CREATE TABLE IF NOT EXISTS payments (
                                        id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount    NUMERIC(19, 2) NOT NULL,
    currency  VARCHAR(10)    NOT NULL,
    status    VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    method    VARCHAR(30)    NOT NULL,
    timestamp TIMESTAMP      NOT NULL DEFAULT NOW(),
    details   TEXT
    );

-- Tabela de Transações
CREATE TABLE IF NOT EXISTS transactions (
                                            id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type       VARCHAR(20)    NOT NULL,
    amount     NUMERIC(19, 2) NOT NULL,
    timestamp  TIMESTAMP      NOT NULL DEFAULT NOW(),
    status     VARCHAR(10)    NOT NULL DEFAULT 'SUCCESS',
    details    TEXT,
    payment_id UUID           NOT NULL REFERENCES payments(id)
    );

-- Index para busca por payment_id
CREATE INDEX IF NOT EXISTS idx_transactions_payment_id
    ON transactions(payment_id);
