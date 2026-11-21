
-- ==========================================
-- BANKING TRANSACTION SYSTEM
-- PostgreSQL Database Schema
-- ==========================================

-- 1. CUSTOMER TABLE
CREATE TABLE customer (
    customer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE,
    phone VARCHAR(15) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. ACCOUNT TABLE
CREATE TABLE account (
    account_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id),

    CONSTRAINT chk_account_balance
        CHECK (balance >= 0),

    CONSTRAINT chk_account_type
        CHECK (account_type IN ('SAVINGS', 'CURRENT')),

    CONSTRAINT chk_account_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'CLOSED'))
);

-- 3. BANK TRANSACTION TABLE
CREATE TABLE bank_transaction (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    from_account_id BIGINT,
    to_account_id BIGINT,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES account(account_id),

    CONSTRAINT fk_transaction_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES account(account_id),

    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transaction_type
        CHECK (
            transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')
        ),

    CONSTRAINT chk_transaction_status
        CHECK (
            status IN ('PENDING', 'SUCCESS', 'FAILED', 'REJECTED')
        )
);

-- 4. TRANSACTION AUDIT TABLE
CREATE TABLE transaction_audit (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason TEXT,
    thread_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. FAILED TRANSACTION TABLE
CREATE TABLE failed_transaction (
    failed_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    from_account_id BIGINT,
    to_account_id BIGINT,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    failure_reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- INDEXES
-- ==========================================

CREATE INDEX idx_account_customer
ON account(customer_id);

CREATE INDEX idx_transaction_from_account
ON bank_transaction(from_account_id);

CREATE INDEX idx_transaction_to_account
ON bank_transaction(to_account_id);

CREATE INDEX idx_transaction_created_at
ON bank_transaction(created_at);

CREATE INDEX idx_transaction_status
ON bank_transaction(status);

CREATE INDEX idx_audit_transaction_id
ON transaction_audit(transaction_id);

CREATE INDEX idx_failed_created_at
ON failed_transaction(created_at);

-- ==========================================
-- END OF SCHEMA
-- ==========================================
