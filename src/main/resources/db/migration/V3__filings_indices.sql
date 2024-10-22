-- select count(*) from filings where registry_code = $1
CREATE INDEX filings_registry_code_idx ON filings (registry_code);
-- search fields
CREATE INDEX filings_company_number_filing_date_idx ON filings (company_number, filing_date desc);
CREATE INDEX filings_document_date_idx ON filings (document_date);
CREATE INDEX filings_filing_date_idx ON filings (filing_date);
-- select filing_id from filings where registry_code = $1 and external_filing_id = $2
CREATE INDEX filings_registry_code_external_filing_id_idx ON filings (registry_code, external_filing_id) INCLUDE (filing_id);
