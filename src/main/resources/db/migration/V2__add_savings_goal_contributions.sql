-- SAVINGS_GOAL_CONTRIBUTION table
CREATE TABLE savings_goal_contributions (
    contribution_id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    contribution_date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (goal_id) REFERENCES savings_goals(goal_id) ON DELETE CASCADE
);

-- Indexes for savings goal contribution queries
CREATE INDEX idx_savings_goal_contributions_goal_id ON savings_goal_contributions(goal_id);
CREATE INDEX idx_savings_goal_contributions_date ON savings_goal_contributions(contribution_date DESC);
