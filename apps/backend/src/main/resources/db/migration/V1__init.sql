-- ============================================
-- V1__init.sql
-- 초기 스키마 (Flyway migration)
-- ============================================

-- MEMBER
CREATE TABLE member (
                        member_id BIGSERIAL PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        phone_number TEXT NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        deleted_at TIMESTAMPTZ,
                        last_login_at TIMESTAMPTZ
);

create index ix_member_email_lower  on member (lower(email));
create index ix_member_phone_digits on member ((regexp_replace(phone_number, '\D', '', 'g')));

-- KAKAO ACCOUNT (1:1 with member)
CREATE TABLE kakao_account (
                               kakao_account_id BIGSERIAL PRIMARY KEY,
                               member_id BIGINT NOT NULL UNIQUE,
                               provider_user_id BIGINT NOT NULL UNIQUE,
                               connected_at TIMESTAMPTZ NOT NULL,
                               refresh_token TEXT,
                               profile_json JSONB,
                               CONSTRAINT fk_kakao_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- CALENDAR
CREATE TABLE calendar (
                          calendar_id BIGSERIAL PRIMARY KEY,
                          owner_id BIGINT NOT NULL,
                          name TEXT NOT NULL,
                          type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL',
                          visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          CONSTRAINT fk_calendar_owner FOREIGN KEY (owner_id) REFERENCES member(member_id) ON DELETE CASCADE,
                          CONSTRAINT ck_calendar_type CHECK (type IN ('PERSONAL', 'GROUP')),
                          CONSTRAINT ck_calendar_visibilty CHECK (visibility IN ('PRIVATE', 'PROTECTED', 'PUBLIC'))
);

-- 조회용 인덱스
CREATE INDEX ix_calendar_owner ON calendar(owner_id);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_calendar_updated_at
    BEFORE UPDATE ON calendar
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- CALENDAR_MEMBER
CREATE TABLE calendar_member (
                                 calendar_member_id BIGSERIAL PRIMARY KEY,
                                 calendar_id BIGINT NOT NULL,
                                 member_id BIGINT NOT NULL,
                                 is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                 status VARCHAR(30) NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 responded_at TIMESTAMPTZ,
                                 CONSTRAINT fk_calendar_member_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id) ON DELETE CASCADE,
                                 CONSTRAINT fk_calendar_member_member FOREIGN KEY (member_id)   REFERENCES member(member_id)   ON DELETE CASCADE,
                                 CONSTRAINT uq_calendar_member UNIQUE (calendar_id, member_id),
                                 CONSTRAINT ck_calendar_member_status CHECK (status IN ('INVITED','ACCEPTED','REJECTED'))
);
CREATE UNIQUE INDEX ux_calendar_member_default_per_user
    ON calendar_member (member_id)
    WHERE is_default = true;

-- 조회 인덱스
CREATE INDEX ix_cm_member   ON calendar_member(member_id);
CREATE INDEX ix_cm_calendar ON calendar_member(calendar_id);
CREATE INDEX ix_cm_status   ON calendar_member(status);

-- FRIENDSHIP
CREATE TABLE friendship (
                            friendship_id BIGSERIAL PRIMARY KEY,
                            requester_id BIGINT NOT NULL,
                            addressee_id BIGINT NOT NULL,
                            status VARCHAR(100) NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL,
                            responded_at TIMESTAMPTZ,
                            CONSTRAINT fk_friendship_member1 FOREIGN KEY (requester_id) REFERENCES member(member_id),
                            CONSTRAINT fk_friendship_member2 FOREIGN KEY (addressee_id) REFERENCES member(member_id),
                            CONSTRAINT uq_friendship UNIQUE (requester_id, addressee_id),
                            CONSTRAINT chk_friendship_not_self CHECK (requester_id <> addressee_id),
                            CONSTRAINT chk_friendship_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED')),
                            CONSTRAINT chk_friendship_response_time CHECK ((status = 'PENDING'  AND responded_at IS NULL) OR (status IN ('ACCEPTED','REJECTED') AND responded_at IS NOT NULL))
);
CREATE UNIQUE INDEX ux_friendship_pair_unique ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX ix_friendship_addressee_status ON friendship (addressee_id, status);
CREATE INDEX ix_friendship_requester_status ON friendship (requester_id, status);

-- NOTIFICATION
CREATE TABLE notification (
                              notification_id BIGSERIAL PRIMARY KEY,
                              member_id       BIGINT NOT NULL REFERENCES member(member_id),
                              actor_id        BIGINT NULL  REFERENCES member(member_id),
                              type            VARCHAR(50) NOT NULL
                                  CHECK (type IN (
                                                  'INVITED_TO_CALENDAR',
                                                  'INVITED_TO_SCHEDULE',
                                                  'RECEIVED_FRIEND_REQUEST',
                                                  'SYSTEM'
                                      )),
                              resource_id     BIGINT NULL, -- calendar_member_id / schedule_participant_id / friendship_id 등
                              payload_json    JSONB NOT NULL DEFAULT '{}'::jsonb,
                              read_at         TIMESTAMPTZ NULL,
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                              dedupe_key      TEXT NULL UNIQUE
);

-- 인덱스: 미읽음 알림 조회 & 최신순 조회 빠르게
CREATE INDEX ix_notification_member_unread
    ON notification(member_id, read_at)
    WHERE read_at IS NULL;

CREATE INDEX ix_notification_member_created
    ON notification(member_id, created_at DESC);

-- 인덱스: 미읽음/최근 정렬 빠르게
CREATE INDEX ix_notif_member_unread ON notification(member_id, read_at) WHERE read_at IS NULL;
CREATE INDEX ix_notif_member_created ON notification(member_id, created_at DESC);

-- PLACE
CREATE TABLE place (
                       place_id BIGSERIAL PRIMARY KEY,
                       provider VARCHAR(20) NOT NULL DEFAULT 'NAVER',
                       provider_place_key TEXT NOT NULL DEFAULT '',
                       title TEXT NOT NULL,
                       link TEXT,
                       category VARCHAR(100),
                       description TEXT,
                       address TEXT,
                       road_address TEXT,
                       lat NUMERIC(9,6) NOT NULL,
                       lng NUMERIC(9,6) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL,
                       CONSTRAINT ck_place_lat CHECK (lat >= -90 AND lat <= 90),
                       CONSTRAINT ck_place_lng CHECK (lng >= -180 AND lng <= 180)
);
CREATE UNIQUE INDEX ux_place_provider_key
    ON place(provider, provider_place_key)
    WHERE provider <> 'MANUAL';

CREATE INDEX ix_place_title
    ON place USING gin (to_tsvector('simple', coalesce(title,'')));
CREATE INDEX ix_place_road_addr
    ON place USING gin (to_tsvector('simple', coalesce(road_address,'')));

CREATE INDEX ix_place_lat_lng ON place(lat, lng);

CREATE TRIGGER tr_place_updated_at
    BEFORE UPDATE ON place
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- SCHEDULE
CREATE TABLE schedule (
                          schedule_id BIGSERIAL PRIMARY KEY,
                          calendar_id BIGINT NOT NULL,
                          title TEXT NOT NULL,
                          memo TEXT,
                          theme VARCHAR(100) NOT NULL DEFAULT 'BLACK',
                          start_at TIMESTAMPTZ NOT NULL,
                          end_at TIMESTAMPTZ NOT NULL,
                          created_by BIGINT NOT NULL,
                          updated_by BIGINT,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          source_schedule_id BIGINT,
                          CONSTRAINT fk_schedule_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id),
                          CONSTRAINT fk_schedule_source FOREIGN KEY (source_schedule_id) REFERENCES schedule(schedule_id) ON DELETE SET NULL
);
CREATE INDEX ix_schedule_source_schedule_id
    ON schedule (source_schedule_id);

-- 4) (선택) 자기참조 금지 체크
ALTER TABLE schedule
    ADD CONSTRAINT chk_schedule_source_not_self
        CHECK (source_schedule_id IS NULL OR source_schedule_id <> schedule_id);

-- SCHEDULE_PARTICIPANT
CREATE TABLE schedule_participant (
                                      schedule_participant_id BIGSERIAL PRIMARY KEY,
                                      schedule_id BIGINT NOT NULL,
                                      member_id BIGINT,
                                      name TEXT,
                                      status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
                                      invited_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      responded_at TIMESTAMPTZ,
                                      CONSTRAINT fk_schedule_participant_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
                                      CONSTRAINT fk_schedule_participant_member FOREIGN KEY (member_id) REFERENCES member(member_id),
                                      CONSTRAINT uq_schedule_participant UNIQUE (schedule_id, member_id)
);

-- SCHEDULE_TODO
CREATE TABLE schedule_todo (
                               schedule_todo_id BIGSERIAL PRIMARY KEY,
                               schedule_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               is_done BOOLEAN NOT NULL DEFAULT FALSE,
                               order_no INTEGER NOT NULL DEFAULT 0,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at TIMESTAMPTZ,
                               CONSTRAINT fk_schedule_todo FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE
);

CREATE INDEX idx_todo_schedule_order ON schedule_todo(schedule_id, order_no, schedule_todo_id);
CREATE UNIQUE INDEX uq_todo_schedule_order ON schedule_todo(schedule_id, order_no);

-- SCHEDULE_REMINDER
CREATE TABLE schedule_reminder (
                                   schedule_reminder_id BIGSERIAL PRIMARY KEY,
                                   schedule_id BIGINT NOT NULL,
                                   minutes_before INTEGER NOT NULL,
                                   CONSTRAINT fk_schedule_reminder FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
                                   CONSTRAINT uq_schedule_reminder UNIQUE (schedule_id, minutes_before)
);

-- SCHEDULE_RECURRENCE
CREATE TABLE schedule_recurrence (
                                     schedule_recurrence_id BIGSERIAL PRIMARY KEY,
                                     schedule_id BIGINT NOT NULL,
                                     freq VARCHAR(50) NOT NULL DEFAULT 'DAILY',
                                     interval_count INT NOT NULL DEFAULT 1,
                                     by_day TEXT[],
                                     by_monthday INT[],
                                     by_month INT[],
                                     until TIMESTAMPTZ NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     CONSTRAINT fk_schedule_recurrence FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
                                     CONSTRAINT uq_recur_schedule UNIQUE (schedule_id),
                                     CONSTRAINT chk_interval_pos CHECK (interval_count >= 1)
);

CREATE INDEX idx_recur_schedule ON schedule_recurrence(schedule_id);

-- SCHEDULE_RECURRENCE_EXCEPTION
CREATE TABLE schedule_recurrence_exception (
                                               schedule_recurrence_exception_id BIGSERIAL PRIMARY KEY,
                                               schedule_recurrence_id BIGINT NOT NULL,
                                               exception_date DATE NOT NULL,
                                               CONSTRAINT fk_recurrence_exception FOREIGN KEY (schedule_recurrence_id) REFERENCES schedule_recurrence(schedule_recurrence_id) ON DELETE CASCADE,
                                               CONSTRAINT uq_recur_exdate UNIQUE (schedule_recurrence_id, exception_date)
);

-- SCHEDULE_PLACE
CREATE TABLE schedule_place (
                                schedule_place_id BIGSERIAL PRIMARY KEY,
                                schedule_id BIGINT NOT NULL,
                                place_id BIGINT,
                                name TEXT,
                                position INTEGER NOT NULL DEFAULT 0,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                CONSTRAINT fk_schedule_place_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
                                CONSTRAINT fk_schedule_place_place FOREIGN KEY (place_id) REFERENCES place(place_id)
);
CREATE UNIQUE INDEX ux_schedule_place
    ON schedule_place(schedule_id, place_id)
    WHERE place_id is not null;
CREATE UNIQUE INDEX ux_schedule_name
    ON schedule_place(schedule_id, name)
    WHERE name is not null;
-- SCHEDULE_LINK
CREATE TABLE schedule_link (
                                schedule_link_id BIGSERIAL PRIMARY KEY,
                                schedule_id BIGINT NOT NULL,
                                url TEXT NOT NULL,
                                label TEXT,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                CONSTRAINT fk_schedule_link_schedule
                                    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_schedule_link_unique ON schedule_link (schedule_id, url);
CREATE INDEX ix_schedule_link_created ON schedule_link (schedule_id, created_at);


-- ATTACHMENT
CREATE TABLE attachment (
                            attachment_id BIGSERIAL PRIMARY KEY,
                            schedule_id BIGINT NOT NULL,
                            original_name TEXT  NOT NULL,
                            object_key TEXT NOT NULL,
                            mime_type TEXT NOT NULL,
                            byte_size BIGINT NOT NULL,
                            position INT NOT NULL DEFAULT 0,
                            created_by BIGINT NOT NULL REFERENCES member(member_id),
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                            is_img BOOLEAN NOT NULL,
                            CONSTRAINT fk_attachment_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE  ,
                            CONSTRAINT fk_attachment_created_by FOREIGN KEY (created_by) REFERENCES member(member_id)
);
CREATE INDEX idx_attachment_schedule ON attachment(schedule_id);

-- CALENDAR_HISTORY
CREATE TABLE calendar_history (
                         calendar_history_id BIGSERIAL PRIMARY KEY,
                         calendar_id BIGINT NOT NULL,
                         actor_id BIGINT NULL,
                         entity_id BIGINT NOT NULL,
                         type           VARCHAR(60) NOT NULL
                             CHECK (type IN (
                                             'CALENDAR_UPDATED',
                                             'SCHEDULE_CREATED','SCHEDULE_UPDATED','SCHEDULE_DELETED',
                                             'SCHEDULE_PARTICIPANT_ADDED','SCHEDULE_PARTICIPANT_REMOVED',
                                             'SCHEDULE_LINK_ADDED','SCHEDULE_LINK_REMOVED',
                                             'SCHEDULE_LOCATION_ADDED','SCHEDULE_LOCATION_UPDATED','SCHEDULE_LOCATION_REMOVED',
                                             'SCHEDULE_ATTACHMENT_ADDED','SCHEDULE_ATTACHMENT_REMOVED'
                                 )),
                         changed_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
                         created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                         CONSTRAINT fk_calendar_history_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id) ON DELETE CASCADE,
                         CONSTRAINT fk_calendar_history_actor FOREIGN KEY (actor_id) REFERENCES member(member_id) ON DELETE SET NULL
);
CREATE INDEX ix_hist_calendar_time   ON calendar_history (calendar_id, created_at DESC);
CREATE INDEX ix_hist_calendar_type   ON calendar_history (calendar_id, type, created_at DESC);

-- EXPENSE
CREATE TABLE expense (
    expense_id BIGSERIAL PRIMARY KEY ,
    schedule_id BIGINT NOT NULL REFERENCES schedule(schedule_id) ON DELETE CASCADE ,
    name VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    paid_at TIMESTAMPTZ DEFAULT now(),
    occurrence_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source in ('MANUAL', 'RECEIPT')),
    receipt_parse_status VARCHAR(15),
    parsed_payload JSONB
);

CREATE INDEX ix_expense_schedule_paidat ON EXPENSE (schedule_id,paid_at DESC);

-- EXPENSE_ATTACHMENT
CREATE TABLE expense_attachment (
    expense_attachment_id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expense(expense_id) ON DELETE CASCADE,
    attachment_id BIGINT NOT NULL REFERENCES attachment(attachment_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_expense_attachment ON expense_attachment(expense_id,attachment_id);
-- EXPENSE_LINE
CREATE TABLE expense_line (
    expense_line_id BIGSERIAL PRIMARY KEY ,
    expense_id BIGINT NOT NULL REFERENCES expense(expense_id) ON DELETE CASCADE,
    label TEXT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_amount BIGINT NOT NULL DEFAULT 0,
    line_amount BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_expenseline_expense ON expense_line(expense_id);

-- EXPENSE_OCR_TASK
CREATE TABLE expense_ocr_task (
    ocr_task_id BIGSERIAL PRIMARY KEY ,
    attachment_id BIGINT NOT NULL REFERENCES attachment(attachment_id) ON DELETE CASCADE,
    schedule_id BIGINT NOT NULL REFERENCES schedule(schedule_id) ON DELETE CASCADE,
    expense_id BIGINT REFERENCES expense(expense_id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED' CHECK ( status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'FAILED')),
    error_message TEXT,
    raw_response JSONB,
    normalized JSONB,
    request_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_ocr_request_hash ON expense_ocr_task (request_hash);
CREATE INDEX ix_expense_ocr_task_attachment ON expense_ocr_task(attachment_id);
CREATE INDEX ix_expense_ocr_task_status     ON expense_ocr_task(status);
CREATE INDEX ix_expense_ocr_task_expense    ON expense_ocr_task(expense_id);
CREATE INDEX ix_expense_ocr_task_status_createdAt ON expense_ocr_task(status,created_at);
