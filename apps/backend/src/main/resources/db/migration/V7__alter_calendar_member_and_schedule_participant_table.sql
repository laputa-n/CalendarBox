ALTER TABLE calendar_member
    ADD COLUMN inviter_id BIGINT,
    ADD CONSTRAINT fk_calendar_member_inviter FOREIGN KEY (inviter_id) REFERENCES member(member_id) ON DELETE SET NULL;

ALTER TABLE schedule_participant
    ADD COLUMN inviter_id BIGINT,
    ADD CONSTRAINT fk_schedule_participant_inviter FOREIGN KEY (inviter_id) REFERENCES member(member_id) ON DELETE SET NULL;

CREATE INDEX ix_calendar_member_inviter_id ON calendar_member(inviter_id);
CREATE INDEX ix_schedule_participant_inviter_id ON schedule_participant(inviter_id);

UPDATE calendar_member cm
SET inviter_id = CASE WHEN cm.member_id = 1 THEN 7 ELSE 1 END
WHERE cm.inviter_id IS NULL;

UPDATE schedule_participant sp
SET inviter_id = CASE WHEN sp.member_id = 1 THEN 7 ELSE 1 END
WHERE sp.inviter_id IS NULL;
