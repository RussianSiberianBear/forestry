-- Метаданные дискового хранения. Содержимое новых файлов в BYTEA больше не записывается.
ALTER TABLE staging.uploaded_files
    ADD COLUMN IF NOT EXISTS sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS relative_path VARCHAR(1000);

-- После внедрения можно освободить старые BLOB-данные отдельной командой после резервной копии:
-- UPDATE staging.uploaded_files SET file_data = NULL WHERE relative_path IS NOT NULL;
-- ALTER TABLE staging.uploaded_files ALTER COLUMN file_data DROP NOT NULL;

-- Контроль повторной загрузки на уровне БД. Если старые дубли уже существуют,
-- сначала удалите/объедините их, затем выполните этот индекс вручную.
CREATE UNIQUE INDEX IF NOT EXISTS ux_uploaded_files_user_sha256
    ON staging.uploaded_files(user_id, sha256)
    WHERE sha256 IS NOT NULL;
