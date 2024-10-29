ALTER TABLE filings
    ADD COLUMN download_time DOUBLE PRECISION,
    ADD COLUMN upload_time DOUBLE PRECISION,
    ADD COLUMN worker_time DOUBLE PRECISION,
    ADD COLUMN total_processing_time DOUBLE PRECISION,
    ADD COLUMN total_uploaded_bytes BIGINT;
