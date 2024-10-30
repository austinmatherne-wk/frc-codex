ALTER TABLE filings
    DROP COLUMN oim_csv_url,
    DROP COLUMN oim_json_url,
    ADD COLUMN oim_directory VARCHAR(100);
