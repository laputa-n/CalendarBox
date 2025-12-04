package com.calendarbox.backend.schedule.repository;

import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
@RequiredArgsConstructor
public class ScheduleEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public void upsertEmbedding(Long scheduleId, float[] embedding){
        String sql = """
            INSERT INTO schedule_embedding (schedule_id, embedding)
            VALUES (?, ?::vector)
            ON CONFLICT (schedule_id)
            DO UPDATE SET embedding = EXCLUDED.embedding
            """;

        PGobject vectorObject = toPgVectorObject(embedding);

        jdbcTemplate.update(sql, scheduleId, vectorObject);
    }

    public void deleteByScheduleId(Long scheduleId) {
        String sql = "DELETE FROM schedule_embedding WHERE schedule_id = ?";
        jdbcTemplate.update(sql, scheduleId);
    }

    public void copyFrom(Long srcScheduleId, Long dstScheduleId) {
        String sql = """
            INSERT INTO schedule_embedding (schedule_id, embedding)
            SELECT ?, embedding
            FROM schedule_embedding
            WHERE schedule_id = ?
            ON CONFLICT (schedule_id)
            DO UPDATE SET embedding = EXCLUDED.embedding
            """;
        jdbcTemplate.update(sql, dstScheduleId, srcScheduleId);
    }

    private PGobject toPgVectorObject(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');

        PGobject obj = new PGobject();
        obj.setType("vector");
        try {
            obj.setValue(sb.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set pgvector value", e);
        }
        return obj;
    }
}
