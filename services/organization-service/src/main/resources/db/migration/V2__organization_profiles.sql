CREATE TABLE organization_profiles (
    organization_id UUID PRIMARY KEY REFERENCES organizations(id) ON DELETE CASCADE,
    entity_type VARCHAR(30) NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    registration_number VARCHAR(100),
    tax_id VARCHAR(100),
    jurisdiction VARCHAR(100),
    incorporation_date DATE,
    industry VARCHAR(100),
    company_size VARCHAR(50),
    stage VARCHAR(50),
    website VARCHAR(255),
    linkedin_url VARCHAR(255),
    country VARCHAR(100),
    city VARCHAR(100),
    address VARCHAR(255),
    postal_code VARCHAR(30),
    signatory_name VARCHAR(150),
    signatory_title VARCHAR(100),
    signatory_email VARCHAR(200),
    signatory_phone VARCHAR(50),
    incorporation_doc_name VARCHAR(255),
    tax_doc_name VARCHAR(255),
    pitch_deck_doc_name VARCHAR(255),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_org_profile_entity_type ON organization_profiles(entity_type);

