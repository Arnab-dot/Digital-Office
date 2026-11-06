-- ===================================================================
-- V2: Notification System Upgrade: Enhanced notifications, preferences,
--     activity feed, immutable audit logs, and processed events
-- ===================================================================

-- 1. Upgrade notifications table
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS organization_id UUID,
    ADD COLUMN IF NOT EXISTS event_id UUID,
    ADD COLUMN IF NOT EXISTS category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS title VARCHAR(200) NOT NULL DEFAULT 'New notification',
    ADD COLUMN IF NOT EXISTS message TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_notif_org ON notifications(organization_id);
CREATE INDEX IF NOT EXISTS ix_notif_category ON notifications(category);
CREATE INDEX IF NOT EXISTS ix_notif_event ON notifications(event_id);

-- 2. User notification preferences
CREATE TABLE IF NOT EXISTS notification_preferences (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL UNIQUE,
    in_app_enabled       BOOLEAN NOT NULL DEFAULT true,
    email_enabled        BOOLEAN NOT NULL DEFAULT true,
    real_time_enabled    BOOLEAN NOT NULL DEFAULT true,
    category_preferences TEXT,
    quiet_hours_start    VARCHAR(10),
    quiet_hours_end      VARCHAR(10),
    time_zone            VARCHAR(50) NOT NULL DEFAULT 'UTC',
    digest_frequency     VARCHAR(20) NOT NULL DEFAULT 'INSTANT',
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_notif_pref_user ON notification_preferences(user_id);

-- 3. Centralized organization and project activity feed
CREATE TABLE IF NOT EXISTS activity_feed (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id      UUID,
    actor_id        UUID NOT NULL,
    category        VARCHAR(40) NOT NULL,
    event_type      VARCHAR(60) NOT NULL,
    resource_type   VARCHAR(40) NOT NULL,
    resource_id     UUID NOT NULL,
    description     TEXT NOT NULL,
    metadata        TEXT,
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_act_feed_org_created ON activity_feed(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_act_feed_proj_created ON activity_feed(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_act_feed_actor ON activity_feed(actor_id);

-- 4. Immutable audit system
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    actor_id        UUID NOT NULL,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(40) NOT NULL,
    resource_id     UUID NOT NULL,
    source_service  VARCHAR(40) NOT NULL,
    outcome         VARCHAR(20) NOT NULL,
    correlation_id  VARCHAR(100),
    metadata        TEXT,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_audit_org_created ON audit_logs(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS ix_audit_actor ON audit_logs(actor_id);

-- 5. Processed events for idempotent consumer delivery
CREATE TABLE IF NOT EXISTS processed_events (
    event_id        UUID PRIMARY KEY,
    source_service  VARCHAR(40),
    event_type      VARCHAR(60),
    processed_at    TIMESTAMPTZ NOT NULL
);

