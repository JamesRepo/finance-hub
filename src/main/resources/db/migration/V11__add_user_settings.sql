-- User settings table for storing user preferences
CREATE TABLE user_settings (
    settings_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    locale VARCHAR(10) DEFAULT 'en-GB',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);

-- Insert default settings for existing users
INSERT INTO user_settings (user_id, currency, locale)
SELECT user_id, 'GBP', 'en-GB' FROM users;
