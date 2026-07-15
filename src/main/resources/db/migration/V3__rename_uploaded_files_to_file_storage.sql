-- Перевод старой таблицы файлов на хранение метаданных.
-- Содержимое файлов теперь хранится только на диске.

ALTER TABLE staging.uploaded_files RENAME TO file_storage;

ALTER TABLE staging.file_storage
    RENAME COLUMN original_filename TO original_name;

ALTER TABLE staging.file_storage
    RENAME COLUMN file_type TO type;

ALTER TABLE staging.file_storage
    RENAME COLUMN file_size TO size;

ALTER TABLE staging.file_storage
    RENAME COLUMN upload_date TO created_at;

ALTER TABLE staging.file_storage
    ADD COLUMN IF NOT EXISTS stored_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(150),
    ADD COLUMN IF NOT EXISTS extension VARCHAR(30),
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(2000);

UPDATE staging.file_storage
SET stored_name = CASE
        WHEN extension IS NOT NULL AND extension <> '' THEN 'source.' || lower(extension)
        WHEN position('.' IN reverse(original_name)) > 0 THEN 'source.' || lower(split_part(original_name, '.', array_length(string_to_array(original_name, '.'), 1)))
        ELSE 'source'
    END
WHERE stored_name IS NULL;

UPDATE staging.file_storage
SET extension = CASE
        WHEN position('.' IN reverse(original_name)) > 0
            THEN lower(split_part(original_name, '.', array_length(string_to_array(original_name, '.'), 1)))
        ELSE ''
    END
WHERE extension IS NULL;

UPDATE staging.file_storage
SET type = CASE
        WHEN lower(extension) = 'zip' THEN 'ARCHIVE'
        WHEN lower(extension) = 'kml' THEN 'KML'
        WHEN lower(extension) = 'kmz' THEN 'KMZ'
        ELSE COALESCE(NULLIF(type, ''), 'DOCUMENT')
    END;

UPDATE staging.file_storage SET processed = false WHERE processed IS NULL;
UPDATE staging.file_storage SET relative_path = 'legacy/unavailable/' || id WHERE relative_path IS NULL OR relative_path = '';
UPDATE staging.file_storage SET sha256 = 'legacy-' || md5(id::text || ':' || original_name) WHERE sha256 IS NULL OR sha256 = '';

ALTER TABLE staging.file_storage
    ALTER COLUMN stored_name SET NOT NULL,
    ALTER COLUMN relative_path SET NOT NULL,
    ALTER COLUMN sha256 SET NOT NULL,
    ALTER COLUMN processed SET NOT NULL;

DROP INDEX IF EXISTS staging.ux_uploaded_files_user_sha256;
CREATE UNIQUE INDEX IF NOT EXISTS ux_file_storage_user_sha256
    ON staging.file_storage(user_id, sha256);

ALTER TABLE staging.file_storage DROP COLUMN IF EXISTS file_data;
ALTER TABLE staging.file_storage DROP COLUMN IF EXISTS archive_id;
