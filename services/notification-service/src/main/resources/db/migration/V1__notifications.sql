CREATE TABLE notifications (
 id UUID PRIMARY KEY, recipient_id UUID NOT NULL, event_type VARCHAR(40) NOT NULL,
 reference_id UUID NOT NULL, reference_type VARCHAR(30) NOT NULL, extra_data TEXT,
 read_flag BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_notifications_recipient_read ON notifications(recipient_id, read_flag);
CREATE INDEX ix_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
