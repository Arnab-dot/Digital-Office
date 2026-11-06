CREATE TABLE document_reviews (
    id UUID PRIMARY KEY,
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    reviewer_id UUID NOT NULL,
    decision VARCHAR(10) NOT NULL,
    feedback TEXT,
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_review_decision CHECK (decision IN ('APPROVED','REJECTED')),
    CONSTRAINT uk_reviewer_version_decision UNIQUE(document_version_id, reviewer_id)
);
CREATE INDEX ix_document_reviews_version ON document_reviews(document_version_id);
