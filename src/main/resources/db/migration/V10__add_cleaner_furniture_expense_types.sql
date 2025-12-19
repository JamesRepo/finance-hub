-- Add CLEANER and FURNITURE to housing expense types

ALTER TABLE housing_expenses DROP CONSTRAINT housing_expenses_expense_type_check;

ALTER TABLE housing_expenses ADD CONSTRAINT housing_expenses_expense_type_check
    CHECK (expense_type IN ('RENT', 'MORTGAGE', 'COUNCIL_TAX', 'ENERGY', 'INTERNET', 'WATER', 'INSURANCE', 'UTILITIES', 'MAINTENANCE', 'HOA_FEES', 'CLEANER', 'FURNITURE', 'OTHER'));
