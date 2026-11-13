ALTER TABLE import_execution ALTER COLUMN finished_at DROP NOT NULL;
ALTER TABLE import_execution ALTER COLUMN source_read_status DROP NOT NULL;