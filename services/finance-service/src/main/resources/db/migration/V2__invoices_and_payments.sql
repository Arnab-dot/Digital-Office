-- ===================================================================
-- V2: Invoices, line items, payments, refunds
-- ===================================================================

-- Invoices
CREATE TABLE invoices (
    id                  UUID PRIMARY KEY,
    invoice_number      VARCHAR(50)   NOT NULL UNIQUE,
    organization_id     UUID          NOT NULL,
    subscription_id     UUID          REFERENCES organization_subscriptions(id),
    status              VARCHAR(20)   NOT NULL, -- DRAFT, ISSUED, PAID, OVERDUE, VOID, PARTIALLY_PAID
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    subtotal            NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_rate            NUMERIC(5,4)  NOT NULL DEFAULT 0, -- e.g. 0.1800 = 18%
    tax_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    total               NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_paid         NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_due          NUMERIC(19,4) NOT NULL DEFAULT 0,
    billing_period_start TIMESTAMPTZ,
    billing_period_end  TIMESTAMPTZ,
    issued_at           TIMESTAMPTZ,
    due_date            TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    version             INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_invoice_org ON invoices(organization_id);
CREATE INDEX ix_invoice_status ON invoices(status);

-- Invoice line items
CREATE TABLE invoice_line_items (
    id          UUID PRIMARY KEY,
    invoice_id  UUID          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(255)  NOT NULL,
    quantity    INT           NOT NULL DEFAULT 1,
    unit_price  NUMERIC(19,4) NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_line_items_invoice ON invoice_line_items(invoice_id);

-- Payment records
CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    organization_id     UUID          NOT NULL,
    invoice_id          UUID          REFERENCES invoices(id),
    payment_method      VARCHAR(30)   NOT NULL, -- MANUAL, BANK_TRANSFER, CARD, OTHER
    status              VARCHAR(20)   NOT NULL, -- PENDING, CONFIRMED, FAILED, REFUNDED
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    reference_number    VARCHAR(100),
    idempotency_key     VARCHAR(100)  UNIQUE,
    confirmed_by        UUID,
    confirmed_at        TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_payment_org ON payments(organization_id);
CREATE INDEX ix_payment_invoice ON payments(invoice_id);

-- Refund records
CREATE TABLE refunds (
    id              UUID PRIMARY KEY,
    payment_id      UUID          NOT NULL REFERENCES payments(id),
    organization_id UUID          NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    reason          TEXT,
    status          VARCHAR(20)   NOT NULL, -- PENDING, PROCESSED, REJECTED
    processed_by    UUID,
    processed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_refund_payment ON refunds(payment_id);
CREATE INDEX ix_refund_org ON refunds(organization_id);
