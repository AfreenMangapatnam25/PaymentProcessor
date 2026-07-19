-- ============================================================================
-- Baseline permissions, roles and a sample ABAC policy.
-- Uses gen_random_uuid() (built into PostgreSQL 13+).
-- ============================================================================

INSERT INTO permission (id, name, resource, action, description) VALUES
    (gen_random_uuid(), 'transaction:read',   'transaction', 'read',   'View transactions'),
    (gen_random_uuid(), 'transaction:create', 'transaction', 'create', 'Create/authorize transactions'),
    (gen_random_uuid(), 'transaction:refund', 'transaction', 'refund', 'Refund transactions'),
    (gen_random_uuid(), 'authorization:read',    'authorization', 'read',    'View authorizations'),
    (gen_random_uuid(), 'authorization:create',  'authorization', 'create',  'Create authorizations'),
    (gen_random_uuid(), 'authorization:capture', 'authorization', 'capture', 'Capture authorizations'),
    (gen_random_uuid(), 'authorization:reverse', 'authorization', 'reverse', 'Reverse/void authorizations'),
    (gen_random_uuid(), 'user:read',   'user',   'read',   'View users'),
    (gen_random_uuid(), 'user:update', 'user',   'update', 'Update users'),
    (gen_random_uuid(), 'merchant:admin', 'merchant', 'admin', 'Full merchant administration'),
    (gen_random_uuid(), 'role:manage', 'role', 'manage', 'Manage roles and permissions');

-- Roles
INSERT INTO role (id, name, description, category, system_role) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',     'Platform super administrator', 'PLATFORM', TRUE),
    (gen_random_uuid(), 'PLATFORM_AUDITOR','Read-only platform auditor',   'PLATFORM', TRUE),
    (gen_random_uuid(), 'SYSTEM',          'Internal system identity',     'PLATFORM', TRUE),
    (gen_random_uuid(), 'MERCHANT_OWNER',  'Merchant owner',               'MERCHANT', TRUE),
    (gen_random_uuid(), 'MERCHANT_ADMIN',  'Merchant administrator',       'MERCHANT', TRUE),
    (gen_random_uuid(), 'MERCHANT_VIEWER', 'Merchant read-only user',      'MERCHANT', TRUE),
    (gen_random_uuid(), 'MERCHANT_BILLING','Merchant billing user',        'MERCHANT', TRUE),
    (gen_random_uuid(), 'END_USER',        'Standard end user',            'USER',     TRUE),
    (gen_random_uuid(), 'PREMIUM_USER',    'Premium end user',             'USER',     TRUE);

-- SUPER_ADMIN -> every permission
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r CROSS JOIN permission p WHERE r.name = 'SUPER_ADMIN';

-- PLATFORM_AUDITOR -> all read permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.action = 'read'
WHERE r.name = 'PLATFORM_AUDITOR';

-- MERCHANT_ADMIN -> transaction + authorization + merchant admin
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
  ON p.name IN ('transaction:read','transaction:create','transaction:refund',
                'authorization:read','authorization:create','authorization:capture',
                'authorization:reverse','merchant:admin')
WHERE r.name = 'MERCHANT_ADMIN';

-- MERCHANT_OWNER inherits MERCHANT_ADMIN and adds role management
UPDATE role owner SET parent_role_id = admin.id
FROM role admin WHERE owner.name = 'MERCHANT_OWNER' AND admin.name = 'MERCHANT_ADMIN';
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.name = 'role:manage'
WHERE r.name = 'MERCHANT_OWNER';

-- MERCHANT_VIEWER -> read only
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
  ON p.name IN ('transaction:read','authorization:read')
WHERE r.name = 'MERCHANT_VIEWER';

-- END_USER -> own profile
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.name IN ('user:read','user:update')
WHERE r.name = 'END_USER';

-- Sample ABAC policy: allow high-value transaction create only for verified merchant admins
INSERT INTO policy (id, name, type, effect, resource, action, condition_json, priority, enabled)
VALUES (
    gen_random_uuid(),
    'high-value-transaction-guard',
    'ABAC',
    'ALLOW',
    'transaction',
    'create',
    '[{"attribute":"subject.kyc_status","operator":"EQUALS","value":"VERIFIED"},{"attribute":"environment.device_trust_level","operator":"GTE","value":0.8}]',
    50,
    TRUE
);
