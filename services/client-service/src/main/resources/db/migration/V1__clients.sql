CREATE TABLE clients (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, name VARCHAR(200) NOT NULL, description TEXT,
 contact_email VARCHAR(320), status VARCHAR(20) NOT NULL, created_by UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_clients_org_status ON clients(organization_id,status);
