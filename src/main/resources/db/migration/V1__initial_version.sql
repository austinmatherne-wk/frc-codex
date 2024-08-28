CREATE TABLE IF NOT EXISTS filings (
    -- Database-generated fields
    filing_id uuid NOT NULL DEFAULT gen_random_uuid(),
    discovered_date DATE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Required fields
    status VARCHAR(50) NOT NULL,
    registry_code VARCHAR(20) NOT NULL,
    download_url VARCHAR(500) NOT NULL,
    -- Fields that may or may not be known before processing
    company_name VARCHAR(100),
    company_number VARCHAR(10),
    lei VARCHAR(20),
    filename VARCHAR(255),
    filing_type VARCHAR(20),
    filing_date DATE,
    document_date DATE,
    stream_timepoint BIGINT,
    -- Result assets
    stub_viewer_url VARCHAR(500),
    oim_csv_url VARCHAR(500),
    oim_json_url VARCHAR(500),

    PRIMARY KEY (filing_id)
);
CREATE INDEX company_name_idx ON filings (company_name);
CREATE INDEX company_number_idx ON filings (company_number);
CREATE INDEX lei_idx ON filings (lei);
