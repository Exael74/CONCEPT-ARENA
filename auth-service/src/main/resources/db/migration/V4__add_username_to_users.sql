-- Registration now requires a username (selected at sign-up, editable later from the profile).
-- Existing rows (if any) are backfilled with a generated placeholder before the column is locked
-- down to NOT NULL + UNIQUE, so this migration is safe to run against a DB that already has users.
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(20);
UPDATE users SET username = 'user_' || substring(id, 1, 8) WHERE username IS NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
