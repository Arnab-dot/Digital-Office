CREATE TABLE workstreams (
 id UUID PRIMARY KEY, project_id UUID NOT NULL, name VARCHAR(200) NOT NULL, description TEXT,
 status VARCHAR(20) NOT NULL, created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_workstreams_project_status ON workstreams(project_id,status);
CREATE TABLE milestones (
 id UUID PRIMARY KEY, project_id UUID NOT NULL, workstream_id UUID REFERENCES workstreams(id),
 title VARCHAR(240) NOT NULL, description TEXT, due_date DATE, status VARCHAR(20) NOT NULL,
 created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_milestones_project ON milestones(project_id);
