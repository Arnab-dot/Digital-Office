CREATE TABLE tasks (
 id UUID PRIMARY KEY, project_id UUID NOT NULL, title VARCHAR(240) NOT NULL, description TEXT,
 status VARCHAR(20) NOT NULL, priority VARCHAR(30) NOT NULL, assignee_id UUID, due_date DATE, created_by UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_tasks_project_status ON tasks(project_id,status);
