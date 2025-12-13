ALTER TABLE users
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN users.is_active IS 'Indicates if the user account is active';

CREATE INDEX idx_users_is_active ON users(is_active);

UPDATE users SET is_active = true WHERE is_active IS NULL;
