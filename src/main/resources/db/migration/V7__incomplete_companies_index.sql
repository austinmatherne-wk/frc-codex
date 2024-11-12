-- select * from companies where completed_date is null order by discovered_date limit ?
CREATE INDEX companies_completed_date_discovered_date_idx ON companies (discovered_date) WHERE completed_date IS NULL;
-- select max(stream_timepoint) from filings
CREATE INDEX filings_stream_timepoint_idx ON filings (stream_timepoint);
