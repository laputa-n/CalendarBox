CREATE INDEX IF NOT EXISTS ix_schedule_member_started_at
    ON schedule (created_by, (date_trunc('month', start_at AT TIME ZONE 'Asia/Seoul')));

-- 지출 발생일 기준 월별/연도별 집계용
CREATE INDEX IF NOT EXISTS ix_expense_schedule_paidat_month
    ON expense (schedule_id, (date_trunc('month', paid_at AT TIME ZONE 'Asia/Seoul')));

-- 장소 통계용
CREATE INDEX IF NOT EXISTS ix_schedule_place_schedule
    ON schedule_place (schedule_id, place_id);

-- 참가자/친구 통계용
CREATE INDEX IF NOT EXISTS ix_schedule_participant_schedule
    ON schedule_participant (schedule_id, member_id);