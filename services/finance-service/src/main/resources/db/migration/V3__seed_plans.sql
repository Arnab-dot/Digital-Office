-- ===================================================================
-- V3: Seed default subscription plans and features
-- ===================================================================

INSERT INTO subscription_plans (
    id, name, display_name, description, tier, billing_interval,
    price, currency, max_members, max_projects, storage_limit_mb,
    max_meeting_participants, max_meeting_duration_min, ai_requests_limit,
    document_limit, is_active, trial_days, created_at, updated_at
) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'free',
    'Free Plan',
    'Ideal for small teams getting started with collaboration.',
    'FREE',
    'MONTHLY',
    0.0000,
    'USD',
    5,
    3,
    500,
    10,
    60,
    50,
    100,
    true,
    0,
    NOW(),
    NOW()
),
(
    '22222222-2222-2222-2222-222222222222',
    'starter',
    'Starter Plan',
    'Perfect for growing startups and agile teams.',
    'STARTER',
    'MONTHLY',
    29.0000,
    'USD',
    15,
    15,
    5000,
    25,
    120,
    500,
    1000,
    true,
    14,
    NOW(),
    NOW()
),
(
    '33333333-3333-3333-3333-333333333333',
    'professional',
    'Professional Plan',
    'Advanced capabilities for established companies and agencies.',
    'PROFESSIONAL',
    'MONTHLY',
    99.0000,
    'USD',
    50,
    100,
    50000,
    50,
    240,
    2500,
    10000,
    true,
    14,
    NOW(),
    NOW()
),
(
    '44444444-4444-4444-4444-444444444444',
    'enterprise',
    'Enterprise Plan',
    'Maximum scale, compliance, custom governance, and dedicated support.',
    'ENTERPRISE',
    'MONTHLY',
    299.0000,
    'USD',
    250,
    1000,
    250000,
    100,
    480,
    10000,
    50000,
    true,
    30,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Seed default features for each plan
INSERT INTO subscription_features (id, plan_id, feature, enabled) VALUES
('a0000001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'BASIC_TASKS', true),
('a0000001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'BASIC_DOCS', true),
('a0000001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'VIDEO_MEETINGS', true),

('a0000002-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'BASIC_TASKS', true),
('a0000002-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'ADVANCED_DOCS', true),
('a0000002-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'VIDEO_MEETINGS', true),
('a0000002-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'AI_ASSISTANT_BASIC', true),

('a0000003-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'ALL_CORE_FEATURES', true),
('a0000003-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333', 'FILE_VERSIONING', true),
('a0000003-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 'MEETING_RECORDING', true),
('a0000003-0000-0000-0000-000000000004', '33333333-3333-3333-3333-333333333333', 'AI_INTELLIGENCE', true),

('a0000004-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'ALL_CORE_FEATURES', true),
('a0000004-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'CUSTOM_ROLES', true),
('a0000004-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', 'AUDIT_LOG_EXPORT', true),
('a0000004-0000-0000-0000-000000000004', '44444444-4444-4444-4444-444444444444', 'DEDICATED_SUPPORT', true),
('a0000004-0000-0000-0000-000000000005', '44444444-4444-4444-4444-444444444444', 'UNLIMITED_AI', true)
ON CONFLICT (id) DO NOTHING;

