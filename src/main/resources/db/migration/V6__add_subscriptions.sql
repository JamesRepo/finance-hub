-- SUBSCRIPTIONS table
CREATE TABLE subscriptions (
    subscription_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    payment_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscription_frequency CHECK (frequency IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_amount CHECK (amount > 0),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Indexes for subscription queries
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_payment_date ON subscriptions(payment_date);
CREATE INDEX idx_subscriptions_user_payment_month ON subscriptions(user_id, payment_date);
