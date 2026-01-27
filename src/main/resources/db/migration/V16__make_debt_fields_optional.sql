-- Make principal_amount optional
ALTER TABLE debts ALTER COLUMN principal_amount DROP NOT NULL;

-- Make start_date optional
ALTER TABLE debts ALTER COLUMN start_date DROP NOT NULL;
