ALTER TABLE organization_profiles ADD COLUMN IF NOT EXISTS allowed_email_domain VARCHAR(100);
ALTER TABLE organization_profiles ADD COLUMN IF NOT EXISTS enforce_company_domain BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS invitation_emails (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    recipient_email VARCHAR(320) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_invitation_emails_org ON invitation_emails(organization_id);

