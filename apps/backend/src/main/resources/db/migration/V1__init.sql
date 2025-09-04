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
                              member_id BIGINT NOT NULL,
                              category VARCHAR(100) NOT NULL,
                              payload_json JSONB NOT NULL,
                              read_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL,
                              CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- PLACE
CREATE TABLE place (
                       place_id BIGSERIAL PRIMARY KEY,
                       title TEXT NOT NULL,
                       link TEXT,
                       category VARCHAR(100),
                       description TEXT,
                       address TEXT,
                       road_address TEXT,
                       map_x NUMERIC(9,6) NOT NULL,
                       map_y NUMERIC(9,6) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL
);

-- SCHEDULE
CREATE TABLE schedule (
                          schedule_id BIGSERIAL PRIMARY KEY,
                          calendar_id BIGINT NOT NULL,
                          title TEXT NOT NULL,
                          memo TEXT,
                          theme VARCHAR(100),
                          start_at TIMESTAMPTZ NOT NULL,
                          end_at TIMESTAMPTZ NOT NULL,
                          external_link_url TEXT,
                          created_by BIGINT NOT NULL,
                          updated_by BIGINT,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          CONSTRAINT fk_schedule_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id)
);

-- SCHEDULE_PARTICIPANT
CREATE TABLE schedule_participant (
                                      schedule_participant_id BIGSERIAL PRIMARY KEY,
                                      schedule_id BIGINT NOT NULL,
                                      member_id BIGINT,
                                      name TEXT,
                                      invited_at TIMESTAMPTZ NOT NULL,
                                      responded_at TIMESTAMPTZ,
                                      is_copied BOOLEAN NOT NULL DEFAULT FALSE,
                                      CONSTRAINT fk_schedule_participant_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
                                      CONSTRAINT fk_schedule_participant_member FOREIGN KEY (member_id) REFERENCES member(member_id),
                                      CONSTRAINT uq_schedule_participant UNIQUE (schedule_id, member_id),
                                      CONSTRAINT uq_schedule_participant2 UNIQUE (schedule_id, name)
);

-- SCHEDULE_TODO
CREATE TABLE schedule_todo (
                               schedule_todo_id BIGSERIAL PRIMARY KEY,
                               schedule_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               is_done BOOLEAN NOT NULL DEFAULT FALSE,
                               order_no INTEGER NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL,
                               updated_at TIMESTAMPTZ NOT NULL,
                               CONSTRAINT fk_schedule_todo FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id)
);

-- SCHEDULE_REMINDER
CREATE TABLE schedule_reminder (
                                   schedule_reminder_id BIGSERIAL PRIMARY KEY,
                                   schedule_id BIGINT NOT NULL,
                                   minutes_before INTEGER NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL,
                                   CONSTRAINT fk_schedule_reminder FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id)
);

-- SCHEDULE_RECURRENCE
CREATE TABLE schedule_recurrence (
                                     schedule_recurrence_id BIGSERIAL PRIMARY KEY,
                                     schedule_id BIGINT NOT NULL,
                                     freq VARCHAR(50) NOT NULL DEFAULT 'DAILY',
                                     interval INT,
                                     by_day VARCHAR(20),
                                     by_monthday INT,
                                     by_month INT,
                                     created_at TIMESTAMPTZ NOT NULL,
                                     end_at TIMESTAMPTZ,
                                     CONSTRAINT fk_schedule_recurrence FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id)
);

-- SCHEDULE_RECURRENCE_EXCEPTION
CREATE TABLE schedule_recurrence_exception (
                                               exception_id BIGSERIAL PRIMARY KEY,
                                               schedule_recurrence_id BIGINT NOT NULL,
                                               exception_date DATE NOT NULL,
                                               created_at TIMESTAMPTZ NOT NULL,
                                               CONSTRAINT fk_recurrence_exception FOREIGN KEY (schedule_recurrence_id) REFERENCES schedule_recurrence(schedule_recurrence_id)
);

-- SCHEDULE_PLACE
CREATE TABLE schedule_place (
                                schedule_place_id BIGSERIAL PRIMARY KEY,
                                schedule_id BIGINT NOT NULL,
                                place_id BIGINT,
                                created_at TIMESTAMPTZ NOT NULL,
                                name TEXT,
                                CONSTRAINT fk_schedule_place_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
                                CONSTRAINT fk_schedule_place_place FOREIGN KEY (place_id) REFERENCES place(place_id)
);

-- ATTACHMENT
CREATE TABLE attachment (
                            attachment_id BIGSERIAL PRIMARY KEY,
                            file_url TEXT NOT NULL,
                            thumbnail_url TEXT,
                            category VARCHAR(100) NOT NULL,
                            size BIGINT NOT NULL,
                            added_by BIGINT NOT NULL,
                            added_at TIMESTAMPTZ NOT NULL,
                            CONSTRAINT fk_attachment_member FOREIGN KEY (added_by) REFERENCES member(member_id)
);

-- SCHEDULE_ATTACHMENT
CREATE TABLE schedule_attachment (
                                     schedule_attachment_id BIGSERIAL PRIMARY KEY,
                                     schedule_id BIGINT NOT NULL,
                                     attachment_id BIGINT NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL,
                                     CONSTRAINT fk_schedule_attachment_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
                                     CONSTRAINT fk_schedule_attachment_attachment FOREIGN KEY (attachment_id) REFERENCES attachment(attachment_id)
);

-- EXPENSE
CREATE TABLE expense (
                         expense_id BIGSERIAL PRIMARY KEY,
                         schedule_id BIGINT NOT NULL,
                         calendar_id BIGINT NOT NULL,
                         payer_id BIGINT NOT NULL,
                         payer_name TEXT NOT NULL,
                         amount DECIMAL(12,2) NOT NULL,
                         source VARCHAR(100),
                         paid_at TIMESTAMPTZ NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL,
                         CONSTRAINT fk_expense_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
                         CONSTRAINT fk_expense_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id),
                         CONSTRAINT fk_expense_member FOREIGN KEY (payer_id) REFERENCES member(member_id)
);

-- EXPENSE_SHARE
CREATE TABLE expense_share (
                               expense_share_id BIGSERIAL PRIMARY KEY,
                               expense_id BIGINT NOT NULL,
                               member_id BIGINT NOT NULL,
                               share_amount DECIMAL(12,2) NOT NULL,
                               CONSTRAINT fk_expense_share_expense FOREIGN KEY (expense_id) REFERENCES expense(expense_id),
                               CONSTRAINT fk_expense_share_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- RECEIPT
CREATE TABLE receipt (
                         receipt_id BIGSERIAL PRIMARY KEY,
                         expense_id BIGINT NOT NULL,
                         attachment_id BIGINT NOT NULL,
                         vendor_name TEXT NOT NULL,
                         vat DECIMAL(12,2) NOT NULL,
                         items_json JSONB NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL,
                         paid_at TIMESTAMPTZ NOT NULL,
                         CONSTRAINT fk_receipt_expense FOREIGN KEY (expense_id) REFERENCES expense(expense_id),
                         CONSTRAINT fk_receipt_attachment FOREIGN KEY (attachment_id) REFERENCES attachment(attachment_id)
);

-- CALENDAR_HISTORY
CREATE TABLE calendar_history (
                                  calendar_history_id BIGSERIAL PRIMARY KEY,
                                  calendar_id BIGINT NOT NULL,
                                  schedule_id BIGINT,
                                  member_id BIGINT NOT NULL,
                                  action VARCHAR(100) NOT NULL,
                                  field VARCHAR(100) NOT NULL,
                                  old_value TEXT,
                                  new_value TEXT,
                                  created_at TIMESTAMPTZ NOT NULL,
                                  CONSTRAINT fk_history_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id),
                                  CONSTRAINT fk_history_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
                                  CONSTRAINT fk_history_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);

