TRUNCATE filings;
INSERT INTO filings(
    discovered_date, status, registry_code, download_url,
    company_name, company_number, external_filing_id, external_view_url,
    filing_date, document_date
)
VALUES (
    CURRENT_TIMESTAMP, 'pending', 'CH',
    'https://find-and-update.company-information.service.gov.uk/company/11162569/filing-history/MzQzOTA3MDIxN2FkaXF6a2N4/document?format=xhtml&download=0',
    'TUSCANY PIZZA LTD', '11162569', 'MzQzOTA3MDIxN2FkaXF6a2N4',
    'https://find-and-update.company-information.service.gov.uk/company/11162569/filing-history/MzQzOTA3MDIxN2FkaXF6a2N4/document?format=xhtml&download=0',
    CURRENT_DATE, CURRENT_DATE
), (
    CURRENT_TIMESTAMP, 'pending', 'CH',
    'https://find-and-update.company-information.service.gov.uk/company/11162569/filing-history/MzM4MTc1MTcwOWFkaXF6a2N4/document?format=xhtml&download=0',
    'TUSCANY PIZZA LTD', '11162569', 'MzM4MTc1MTcwOWFkaXF6a2N4',
    'https://find-and-update.company-information.service.gov.uk/company/11162569/filing-history/MzM4MTc1MTcwOWFkaXF6a2N4/document?format=xhtml&download=0',
    '2023-06-02', '2023-01-31'
);
