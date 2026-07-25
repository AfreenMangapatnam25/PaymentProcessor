-- ============================================================================
-- V4: social_accounts
--
-- Links an external OAuth2/OIDC provider account (Google, GitHub, Microsoft) to
-- a local identity, so federated logins resolve to one stable local account.
--
-- Version note: V3 is taken by the local-only seed migration in db/seed (which is
-- layered onto the Flyway location list under the "local" profile), so this
-- schema migration is V4. Flyway does not require contiguous version numbers.
-- ============================================================================

CREATE TABLE social_accounts (
    id               VARCHAR(36)  PRIMARY KEY,
    identity_id      VARCHAR(36)  NOT NULL REFERENCES identities (id) ON DELETE CASCADE,

    -- SocialProvider enum: GOOGLE | GITHUB | MICROSOFT
    provider         VARCHAR(20)  NOT NULL,

    -- The provider's immutable subject id (OIDC "sub", GitHub numeric id).
    -- Deliberately NOT the email: emails get changed and reassigned, subjects do not.
    provider_user_id VARCHAR(255) NOT NULL,

    -- Cached profile fields, refreshed on every login. Nullable because GitHub
    -- omits the email unless the user granted the user:email scope.
    email            VARCHAR(320),
    display_name     VARCHAR(255),
    avatar_url       VARCHAR(1024),

    linked_at        TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ
);

-- Makes repeat logins idempotent: the second and later logins from the same
-- external account find this row instead of provisioning a duplicate identity.
-- Scoped per-provider so two providers can never collide on the same id value.
CREATE UNIQUE INDEX ux_social_accounts_provider_user
    ON social_accounts (provider, provider_user_id);

-- Supports "list my connected accounts" and the check preventing a user from
-- unlinking their only remaining login method.
CREATE INDEX ix_social_accounts_identity ON social_accounts (identity_id);

-- Supports the verified-email account-linking lookup performed at login time.
CREATE INDEX ix_social_accounts_email ON social_accounts (LOWER(email))
    WHERE email IS NOT NULL;
