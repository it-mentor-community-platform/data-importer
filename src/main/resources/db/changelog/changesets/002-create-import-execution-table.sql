CREATE TABLE IF NOT EXISTS import_execution
(
    id BIGSERIAL PRIMARY KEY,
    type BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    started_at BIGINT NOT NULL,
    finished_at BIGINT NOT NULL,
    success_count INT NOT NULL,
    error_count INT NOT NULL,
    source_read_status VARCHAR(255) NOT NULL,

    CONSTRAINT fk_import_execution_import_type
        FOREIGN KEY (type)
            REFERENCES import_type (id)
            ON DELETE CASCADE
);