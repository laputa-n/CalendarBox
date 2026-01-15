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
                        'SCHEDULE_DELETED',

                        'SCHEDULE_PARTICIPANT_ADDED',
                        'SCHEDULE_PARTICIPANT_REMOVED',

                        'SCHEDULE_LINK_ADDED',
                        'SCHEDULE_LINK_REMOVED',

                        'SCHEDULE_LOCATION_ADDED',
                        'SCHEDULE_LOCATION_UPDATED',
                        'SCHEDULE_LOCATION_REMOVED',

                        'SCHEDULE_ATTACHMENT_ADDED',
                        'SCHEDULE_ATTACHMENT_REMOVED'
            ));
