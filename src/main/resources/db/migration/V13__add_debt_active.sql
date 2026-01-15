-- Add active column to debts table
ALTER TABLE debts ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
