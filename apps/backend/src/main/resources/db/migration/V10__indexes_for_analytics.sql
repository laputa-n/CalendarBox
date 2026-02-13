CREATE INDEX IF NOT EXISTS ix_sp_member_accepted
    ON schedule_participant (member_id, schedule_id)
    WHERE status = 'ACCEPTED';

CREATE INDEX IF NOT EXISTS ix_schedule_created_by
    ON schedule (created_by, schedule_id);