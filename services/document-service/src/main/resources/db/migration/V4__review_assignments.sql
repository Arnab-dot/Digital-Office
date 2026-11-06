CREATE TABLE review_assignments (
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    reviewer_id UUID NOT NULL,
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_review_assignment UNIQUE(document_version_id, reviewer_id)
);
CREATE INDEX ix_review_assignments_version ON review_assignments(document_version_id);
CREATE INDEX ix_review_assignments_reviewer ON review_assignments(reviewer_id);
