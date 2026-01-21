-- Add soft delete column to debts table
ALTER TABLE debts ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add soft delete column to debt_payments table
ALTER TABLE debt_payments ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for filtering non-deleted debts
CREATE INDEX idx_debts_deleted ON debts(deleted);

-- Add index for filtering non-deleted debt payments
CREATE INDEX idx_debt_payments_deleted ON debt_payments(deleted);
