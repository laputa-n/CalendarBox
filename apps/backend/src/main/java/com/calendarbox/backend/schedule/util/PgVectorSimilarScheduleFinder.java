package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PgVectorSimilarScheduleFinder {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;

    public record SimilarSchedule(Long scheduleId, double similarity) {}

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        // 1) 요청으로부터 검색 텍스트 만들기 (필드 이름에 맞게 수정)
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);

        // 2) pgvector literal 만들기
        PGobject vectorObject = toPgVectorObject(queryEmbedding);

        String sql = """
    SELECT schedule_id,
           1 - (embedding <=> ?::vector) AS similarity
    FROM schedule_embedding se
    WHERE EXISTS (
        SELECT 1
        FROM schedule_place sp
        WHERE sp.schedule_id = se.schedule_id
          AND sp.place_id IS NOT NULL
    )
    ORDER BY se.embedding <=> ?::vector
    LIMIT ?
    """;

        // 3) JDBC 쿼리
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SimilarSchedule(
                        rs.getLong("schedule_id"),
                        rs.getDouble("similarity")
                ),
                vectorObject,  // 1번째 ?
                vectorObject,  // 2번째 ? (ORDER BY)
                limit          // LIMIT
        );
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
