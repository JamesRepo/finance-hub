-- Remove unused columns from income_sources table

-- Remove unused columns (keeping next_expected_date as it's used for recurring income logic)
ALTER TABLE income_sources
    DROP COLUMN IF EXISTS source_name,
    DROP COLUMN IF EXISTS amount;
