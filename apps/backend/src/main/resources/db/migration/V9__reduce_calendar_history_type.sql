-- 1) 기존 체크 제약 제거
ALTER TABLE calendar_history
DROP CONSTRAINT IF EXISTS calendar_history_type_check;

-- 2) 새 체크 제약 추가(누락된 2개 추가)
ALTER TABLE calendar_history
    ADD CONSTRAINT calendar_history_type_check
        CHECK (type IN (
                        'CALENDAR_UPDATED',

                        'CALENDAR_MEMBER_ADDED',
                        'CALENDAR_MEMBER_REMOVED',

                        'SCHEDULE_CREATED',
                        'SCHEDULE_UPDATED',
                        'SCHEDULE_DELETED'
            ));
