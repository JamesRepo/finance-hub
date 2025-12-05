-- Add gross_amount and net_amount columns to income_sources
ALTER TABLE income_sources
    ADD COLUMN gross_amount DECIMAL(15, 2),
    ADD COLUMN net_amount DECIMAL(15, 2);

-- Update existing records to use amount as gross_amount
UPDATE income_sources
SET gross_amount = amount,
    net_amount = amount
WHERE gross_amount IS NULL;

-- Make gross_amount required after migration
ALTER TABLE income_sources
    ALTER COLUMN gross_amount SET NOT NULL;

-- INCOME_DEDUCTIONS table
CREATE TABLE income_deductions (
    deduction_id BIGSERIAL PRIMARY KEY,
    income_source_id BIGINT NOT NULL,
    deduction_type VARCHAR(50) NOT NULL,
    deduction_name VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    is_percentage BOOLEAN NOT NULL DEFAULT false,
    percentage_value DECIMAL(5, 2),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (income_source_id) REFERENCES income_sources(income_source_id) ON DELETE CASCADE,
    CONSTRAINT chk_deduction_type CHECK (
        deduction_type IN ('INCOME_TAX', 'NATIONAL_INSURANCE', 'PENSION', 'STUDENT_LOAN',
                          'POSTGRAD_LOAN', 'DENTAL_INSURANCE', 'HEALTH_INSURANCE',
                          'LIFE_INSURANCE', 'GYM_MEMBERSHIP', 'UNION_DUES', 'PARKING', 'OTHER')
    ),
    CONSTRAINT chk_deduction_amount CHECK (amount >= 0),
    CONSTRAINT chk_percentage_logic CHECK (
        (is_percentage = false AND percentage_value IS NULL) OR
        (is_percentage = true AND percentage_value IS NOT NULL AND percentage_value >= 0 AND percentage_value <= 100)
    )
);

-- Indexes for income deduction queries
CREATE INDEX idx_income_deductions_income_source_id ON income_deductions(income_source_id);
CREATE INDEX idx_income_deductions_type ON income_deductions(deduction_type);
