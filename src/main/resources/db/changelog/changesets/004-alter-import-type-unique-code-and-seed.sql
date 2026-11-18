ALTER TABLE import_type
    ADD CONSTRAINT uq_import_type_code UNIQUE (code);

INSERT INTO import_type (code) VALUES
                                   ('USERS'),
                                   ('PROFILES'),
                                   ('PROJECTS'),
                                   ('REVIEWS'),
                                   ('MENTORS'),
                                   ('GUARANTEED_REVIEWS'),
                                   ('QUESTIONS')
ON CONFLICT (code) DO NOTHING;