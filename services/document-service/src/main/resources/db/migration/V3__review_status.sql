ALTER TABLE document_versions ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE document_versions ADD CONSTRAINT ck_review_status CHECK (review_status IN ('DRAFT','IN_REVIEW','APPROVED','REJECTED'));
CREATE INDEX ix_doc_versions_review_status ON document_versions(document_id, review_status);
