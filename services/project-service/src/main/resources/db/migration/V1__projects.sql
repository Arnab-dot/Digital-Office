CREATE TABLE projects (
 id UUID PRIMARY KEY, name VARCHAR(200) NOT NULL, description TEXT, owner_organization_id UUID NOT NULL, client_id UUID,
 status VARCHAR(20) NOT NULL, created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_projects_owner_status ON projects(owner_organization_id,status);
CREATE TABLE project_participations (
 id UUID PRIMARY KEY, project_id UUID NOT NULL REFERENCES projects(id), organization_id UUID NOT NULL,
 role VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL, added_by UUID NOT NULL, joined_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_project_org UNIQUE(project_id,organization_id)
);
