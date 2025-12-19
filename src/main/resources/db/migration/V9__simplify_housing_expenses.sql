-- Simplify housing_expenses for monthly-based tracking

-- Drop the index that references is_active
DROP INDEX IF EXISTS idx_housing_expenses_user_active;

-- Remove unnecessary columns and rename start_date to expense_month
ALTER TABLE housing_expenses
    DROP COLUMN IF EXISTS frequency,
    DROP COLUMN IF EXISTS end_date,
    DROP COLUMN IF EXISTS is_active;

-- Rename start_date to expense_month for clarity
ALTER TABLE housing_expenses RENAME COLUMN start_date TO expense_month;

-- Add index for querying by user and month
CREATE INDEX idx_housing_expenses_user_month ON housing_expenses(user_id, expense_month);
