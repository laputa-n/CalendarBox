-- 1) 자기 자신에게 요청 금지
ALTER TABLE friendship
    ADD CONSTRAINT chk_friendship_not_self
        CHECK (requester_id <> addressee_id);

-- 2) (A,B)와 (B,A) 양방향 중복 방지: 정규화된 쌍 유니크
-- Postgres 함수 기반 인덱스 사용
CREATE UNIQUE INDEX ux_friendship_pair_unique
    ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

-- 3) 조회 성능용 인덱스 (요청 목록/받은 목록/상태별 조회)
-- 받은 요청 빠르게 (수신함)
CREATE INDEX ix_friendship_addressee_status ON friendship (addressee_id, status);

-- 보낸 요청 빠르게 (발신함)
CREATE INDEX ix_friendship_requester_status ON friendship (requester_id, status);

ALTER TABLE friendship
    ADD CONSTRAINT chk_friendship_status
        CHECK (status IN ('PENDING','ACCEPTED','REJECTED'));

ALTER TABLE friendship
    ADD CONSTRAINT chk_friendship_response_time
        CHECK (
            (status = 'PENDING'  AND responded_at IS NULL) OR
            (status IN ('ACCEPTED','REJECTED') AND responded_at IS NOT NULL)
            );