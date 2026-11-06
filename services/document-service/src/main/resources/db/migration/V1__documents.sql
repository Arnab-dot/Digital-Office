CREATE TABLE documents (
 id UUID PRIMARY KEY, project_id UUID NOT NULL, title VARCHAR(240) NOT NULL, description TEXT,
 current_version INTEGER NOT NULL, status VARCHAR(20) NOT NULL, created_by UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_documents_project_status ON documents(project_id,status);
