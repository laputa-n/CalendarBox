ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS embedding_dirty boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS embedding_synced_at timestamptz,
    ADD COLUMN IF NOT EXISTS embedding_fail_count int NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS embedding_last_error text,
    ADD COLUMN IF NOT EXISTS embedding_status varchar(20) NOT NULL DEFAULT 'SYNCED';

ALTER TABLE schedule
    ADD CONSTRAINT chk_schedule_embedding_status
        CHECK (embedding_status IN ('QUEUED','RUNNING','SYNCED','FAILED'));

CREATE INDEX IF NOT EXISTS ix_schedule_embedding_dirty_true
    ON schedule(schedule_id)
    WHERE embedding_dirty = true;
