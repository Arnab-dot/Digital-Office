CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE document_versions (
 id UUID PRIMARY KEY, document_id UUID NOT NULL REFERENCES documents(id), version_number INTEGER NOT NULL,
 created_by UUID NOT NULL, change_summary TEXT, storage_reference VARCHAR(500), created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_document_version_number UNIQUE(document_id,version_number)
);
INSERT INTO document_versions (id, document_id, version_number, created_by, change_summary, created_at)
SELECT gen_random_uuid(), id, current_version, created_by, 'Migrated existing document version', created_at
FROM documents
WHERE current_version >= 0;
