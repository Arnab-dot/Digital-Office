-- ===================================================================
-- V1: Subscription plans, organization subscriptions, features, usage
-- ===================================================================

-- Configurable subscription plans
CREATE TABLE subscription_plans (
    id              UUID PRIMARY KEY,
    name            VARCHAR(80)   NOT NULL UNIQUE,
    display_name    VARCHAR(150)  NOT NULL,
    description     TEXT,
    tier            VARCHAR(30)   NOT NULL, -- FREE, STARTER, PROFESSIONAL, ENTERPRISE, CUSTOM
    billing_interval VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, ANNUAL
    price           NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    max_members     INT           NOT NULL DEFAULT 5,
    max_projects    INT           NOT NULL DEFAULT 3,
    storage_limit_mb BIGINT       NOT NULL DEFAULT 500,
    max_meeting_participants INT  NOT NULL DEFAULT 10,
    max_meeting_duration_min INT  NOT NULL DEFAULT 60,
    ai_requests_limit INT        NOT NULL DEFAULT 0,
    document_limit  INT           NOT NULL DEFAULT 100,
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    trial_days      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);

-- Plan feature flags for fine-grained feature access control
CREATE TABLE subscription_features (
    id      UUID PRIMARY KEY,
    plan_id UUID         NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    feature VARCHAR(80)  NOT NULL, -- e.g. ADVANCED_SEARCH, FILE_VERSIONING, MEETING_RECORDING
    enabled BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uk_plan_feature UNIQUE (plan_id, feature)
);
CREATE INDEX ix_features_plan ON subscription_features(plan_id);

-- Organization subscriptions
CREATE TABLE organization_subscriptions (
    id                  UUID PRIMARY KEY,
    organization_id     UUID         NOT NULL,
    plan_id             UUID         NOT NULL REFERENCES subscription_plans(id),
    status              VARCHAR(20)  NOT NULL, -- TRIAL, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED
    billing_interval    VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    current_period_start TIMESTAMPTZ NOT NULL,
    current_period_end  TIMESTAMPTZ  NOT NULL,
    trial_end           TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_subscription UNIQUE (organization_id)
);
CREATE INDEX ix_sub_status ON organization_subscriptions(status);

-- Subscription history for plan changes, upgrades, downgrades
CREATE TABLE subscription_history (
    id              UUID PRIMARY KEY,
    subscription_id UUID        NOT NULL REFERENCES organization_subscriptions(id) ON DELETE CASCADE,
    organization_id UUID        NOT NULL,
    action          VARCHAR(30) NOT NULL, -- CREATED, UPGRADED, DOWNGRADED, RENEWED, CANCELLED, SUSPENDED, REACTIVATED, TRIAL_STARTED, TRIAL_EXPIRED
    from_plan_id    UUID,
    to_plan_id      UUID,
    actor_id        UUID        NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_sub_history_sub ON subscription_history(subscription_id);
CREATE INDEX ix_sub_history_org ON subscription_history(organization_id);

-- Usage tracking per organization per metric per period
CREATE TABLE subscription_usage (
    id              UUID PRIMARY KEY,
    organization_id UUID        NOT NULL,
    metric          VARCHAR(50) NOT NULL, -- MEMBERS, STORAGE_MB, PROJECTS, MEETINGS, MEETING_MINUTES, AI_REQUESTS, DOCUMENTS, API_CALLS
    current_value   BIGINT      NOT NULL DEFAULT 0,
    period_start    TIMESTAMPTZ NOT NULL,
    period_end      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_org_metric_period UNIQUE (organization_id, metric, period_start)
);
CREATE INDEX ix_usage_org ON subscription_usage(organization_id);
