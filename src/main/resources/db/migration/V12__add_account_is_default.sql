-- Add is_default column to accounts table
ALTER TABLE accounts ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for efficient default account lookup per user
CREATE INDEX idx_accounts_user_default ON accounts (user_id, is_default) WHERE is_default = TRUE;
