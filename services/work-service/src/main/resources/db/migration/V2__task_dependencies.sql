CREATE TABLE task_dependencies (
 id UUID PRIMARY KEY,
 task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
 depends_on_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
 created_by UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_task_dependency UNIQUE(task_id, depends_on_task_id),
 CONSTRAINT ck_task_dependency_not_self CHECK(task_id <> depends_on_task_id)
);
CREATE INDEX ix_task_dependencies_dependency ON task_dependencies(depends_on_task_id);
