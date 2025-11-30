-- pgvector 익스텐션 (이미 있으면 에러 안 나고 그냥 유지됨)
CREATE EXTENSION IF NOT EXISTS vector;

-- 스케줄 임베딩 테이블
CREATE TABLE schedule_embedding (
                                    schedule_id BIGINT PRIMARY KEY
                                        REFERENCES schedule(schedule_id) ON DELETE CASCADE,
                                    embedding vector(384) NOT NULL,          -- 모델 차원 수에 맞춰서 (예: 768, 1024 등)
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 최근 것/조회용 인덱스 (선택)
CREATE INDEX ix_schedule_embedding_updated_at ON schedule_embedding(updated_at DESC);

-- 벡터 검색용 인덱스 (예: cosine distance용)
CREATE INDEX ix_schedule_embedding_vector_cosine
    ON schedule_embedding
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
