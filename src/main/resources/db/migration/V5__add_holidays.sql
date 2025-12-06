-- HOLIDAYS table
CREATE TABLE holidays (
    holiday_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget DECIMAL(15, 2) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_holiday_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_holiday_budget CHECK (budget > 0),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Indexes for holiday queries
CREATE INDEX idx_holidays_user_id ON holidays(user_id);
CREATE INDEX idx_holidays_user_active ON holidays(user_id, is_active);
CREATE INDEX idx_holidays_start_date ON holidays(start_date);

-- HOLIDAY_EXPENSES table
CREATE TABLE holiday_expenses (
    expense_id BIGSERIAL PRIMARY KEY,
    holiday_id BIGINT NOT NULL,
    expense_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    expense_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_expense_type CHECK (
        expense_type IN ('FLIGHT', 'ACCOMMODATION', 'TRAIN', 'BUS', 'CAR_RENTAL', 'TAXI',
                        'BREAKFAST', 'LUNCH', 'DINNER', 'SNACKS', 'LUGGAGE', 'ACTIVITIES',
                        'SHOPPING', 'INSURANCE', 'VISA', 'PARKING', 'FUEL', 'OTHER')
    ),
    CONSTRAINT chk_expense_amount CHECK (amount > 0),
    FOREIGN KEY (holiday_id) REFERENCES holidays(holiday_id) ON DELETE CASCADE
);

-- Indexes for expense queries
CREATE INDEX idx_holiday_expenses_holiday_id ON holiday_expenses(holiday_id);
CREATE INDEX idx_holiday_expenses_expense_date ON holiday_expenses(expense_date);
