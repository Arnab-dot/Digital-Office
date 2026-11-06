CREATE TABLE project_invitations (
 id UUID PRIMARY KEY, project_id UUID NOT NULL REFERENCES projects(id), recipient_organization_id UUID NOT NULL,
 recipient_user_id UUID NOT NULL, invited_by UUID NOT NULL, status VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, resolved_at TIMESTAMPTZ
);
CREATE INDEX ix_project_invitations_recipient ON project_invitations(recipient_user_id,status);
