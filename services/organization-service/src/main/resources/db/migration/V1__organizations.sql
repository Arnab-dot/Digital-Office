CREATE TABLE organizations (
 id UUID PRIMARY KEY, name VARCHAR(150) NOT NULL, slug VARCHAR(80) NOT NULL UNIQUE,
 status VARCHAR(20) NOT NULL, owner_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE organization_memberships (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organizations(id), user_id UUID NOT NULL,
 role VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_org_user UNIQUE (organization_id,user_id)
);
CREATE INDEX ix_membership_user_status ON organization_memberships(user_id,status);
CREATE TABLE organization_invitations (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES organizations(id), invitee_email VARCHAR(320) NOT NULL,
 inviter_id UUID NOT NULL, intended_role VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
 token_hash VARCHAR(128) NOT NULL, expires_at TIMESTAMPTZ NOT NULL, decided_at TIMESTAMPTZ
);
CREATE INDEX ix_invite_email_status ON organization_invitations(invitee_email,status);
