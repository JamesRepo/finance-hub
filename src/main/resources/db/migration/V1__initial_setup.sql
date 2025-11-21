-- USER table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Case-insensitive unique email
CREATE UNIQUE INDEX uq_users_email_ci ON users (lower(email));

-- ACCOUNT table
CREATE TABLE accounts (
    account_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'CASH', 'OTHER')),
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_currency_uppercase CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT unique_user_account_name UNIQUE (user_id, account_name),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Index for faster user account lookups
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_user_active ON accounts(user_id, is_active);

-- CATEGORY table
CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    category_name VARCHAR(100) NOT NULL,
    category_type VARCHAR(20) NOT NULL CHECK (category_type IN ('INCOME', 'EXPENSE')),
    color_code VARCHAR(7),
    is_system BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Case-insensitive uniqueness: per-user category names
CREATE UNIQUE INDEX uq_categories_user_name_ci ON categories (user_id, lower(category_name));
-- System categories must be globally unique by name (case-insensitive)
CREATE UNIQUE INDEX uq_categories_system_name_ci ON categories (lower(category_name)) WHERE is_system = true;

-- Index for category lookups
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_type ON categories(category_type);

-- TRANSACTION table
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    transaction_date DATE NOT NULL,
    description TEXT,
    notes TEXT,
    payment_method VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT
);

-- Indexes for transaction queries (critical for performance)
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date DESC);
CREATE INDEX idx_transactions_account_date ON transactions(account_id, transaction_date DESC);
CREATE INDEX idx_transactions_type_date ON transactions(transaction_type, transaction_date DESC);

-- BUDGET table
CREATE TABLE budgets (
    budget_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    period_type VARCHAR(20) NOT NULL CHECK (period_type IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE,
    CONSTRAINT check_budget_dates CHECK (end_date > start_date)
);

-- Indexes for budget queries
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budgets_category_id ON budgets(category_id);
CREATE INDEX idx_budgets_dates ON budgets(start_date, end_date);

-- DEBT table
CREATE TABLE debts (
    debt_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    debt_name VARCHAR(100) NOT NULL,
    debt_type VARCHAR(50) NOT NULL CHECK (debt_type IN ('CREDIT_CARD', 'STUDENT_LOAN', 'MORTGAGE', 'AUTO_LOAN', 'PERSONAL_LOAN', 'OTHER')),
    principal_amount DECIMAL(15, 2) NOT NULL CHECK (principal_amount > 0),
    current_balance DECIMAL(15, 2) NOT NULL CHECK (current_balance >= 0),
    interest_rate DECIMAL(5, 2) NOT NULL CHECK (interest_rate >= 0),
    start_date DATE NOT NULL,
    target_payoff_date DATE,
    minimum_payment DECIMAL(15, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Index for debt queries
CREATE INDEX idx_debts_user_id ON debts(user_id);

-- DEBT_PAYMENT table
CREATE TABLE debt_payments (
    payment_id BIGSERIAL PRIMARY KEY,
    debt_id BIGINT NOT NULL,
    payment_amount DECIMAL(15, 2) NOT NULL CHECK (payment_amount > 0),
    principal_paid DECIMAL(15, 2) NOT NULL CHECK (principal_paid >= 0),
    interest_paid DECIMAL(15, 2) NOT NULL CHECK (interest_paid >= 0),
    payment_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (debt_id) REFERENCES debts(debt_id) ON DELETE CASCADE,
    CONSTRAINT check_payment_breakdown CHECK (payment_amount = principal_paid + interest_paid)
);

-- Indexes for debt payment queries
CREATE INDEX idx_debt_payments_debt_id ON debt_payments(debt_id);
CREATE INDEX idx_debt_payments_date ON debt_payments(payment_date DESC);

-- SAVINGS_GOAL table
CREATE TABLE savings_goals (
    goal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal_name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(15, 2) NOT NULL CHECK (target_amount > 0),
    current_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00 CHECK (current_amount >= 0),
    target_date DATE,
    priority VARCHAR(20) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Index for savings goal queries
CREATE INDEX idx_savings_goals_user_id ON savings_goals(user_id);

-- HOUSING_EXPENSE table
CREATE TABLE housing_expenses (
    expense_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expense_type VARCHAR(50) NOT NULL CHECK (expense_type IN ('RENT', 'MORTGAGE', 'PROPERTY_TAX', 'INSURANCE', 'UTILITIES', 'MAINTENANCE', 'HOA_FEES', 'OTHER')),
    amount DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'ONE_TIME')),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Index for housing expense queries
CREATE INDEX idx_housing_expenses_user_id ON housing_expenses(user_id);
CREATE INDEX idx_housing_expenses_user_active ON housing_expenses(user_id, is_active);
