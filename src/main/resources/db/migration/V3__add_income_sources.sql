-- INCOME_SOURCES table
CREATE TABLE income_sources (
    income_source_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    description TEXT,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    is_recurring BOOLEAN NOT NULL DEFAULT false,
    recurrence_frequency VARCHAR(20),
    start_date DATE NOT NULL,
    end_date DATE,
    next_expected_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    category_id BIGINT,
    auto_create_transaction BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    CONSTRAINT chk_recurrence_frequency CHECK (
        recurrence_frequency IN ('ONE_TIME', 'WEEKLY', 'BI_WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUALLY', 'ANNUALLY')
    ),
    CONSTRAINT chk_recurring_has_frequency CHECK (
        (is_recurring = false) OR (is_recurring = true AND recurrence_frequency IS NOT NULL)
    ),
    CONSTRAINT chk_end_date_after_start CHECK (
        end_date IS NULL OR end_date >= start_date
    )
);

-- Indexes for income source queries
CREATE INDEX idx_income_sources_user_id ON income_sources(user_id);
CREATE INDEX idx_income_sources_active ON income_sources(is_active);
CREATE INDEX idx_income_sources_recurring ON income_sources(is_recurring);
CREATE INDEX idx_income_sources_next_date ON income_sources(next_expected_date);
