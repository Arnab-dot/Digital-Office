CREATE TABLE document_activity_events (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    subject_id UUID NOT NULL,
    subject_type VARCHAR(30) NOT NULL,
    metadata TEXT,
    event_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_doc_activity_project_time ON document_activity_events(project_id, event_at DESC);
CREATE INDEX ix_doc_activity_subject ON document_activity_events(subject_id);
