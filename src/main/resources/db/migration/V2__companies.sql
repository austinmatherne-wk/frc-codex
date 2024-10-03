CREATE TABLE IF NOT EXISTS companies (
    company_number VARCHAR(20) NOT NULL,
    discovered_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMPTZ,
    company_name VARCHAR(100),

    PRIMARY KEY (company_number)
);
CREATE INDEX companies_idx ON companies (company_number);
