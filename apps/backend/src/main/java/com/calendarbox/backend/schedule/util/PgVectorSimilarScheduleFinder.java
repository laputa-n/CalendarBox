package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSimilarScheduleFinder {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;

    public record SimilarSchedule(Long scheduleId, double similarity) {}

    private final RowMapper<SimilarSchedule> rowMapper =
            (rs, rowNum) -> new SimilarSchedule(
                    rs.getLong("schedule_id"),
                    rs.getDouble("similarity")
            );

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        // 1) 검색용 임베딩 생성
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);

        // 2) query vector도 INSERT 때처럼 PGobject(vector)로 보냄
        PGobject qvec = toPgVectorObject(queryEmbedding);

        jdbcTemplate.execute("SET enable_indexscan = on");

        // 3) 실제 유사 스케줄 조회 (cast 절대 쓰지 말 것)
        String sql = """
            SELECT se.schedule_id,
                   1 - (se.embedding <=> ?) AS similarity
            FROM schedule_embedding se
            WHERE EXISTS (
                SELECT 1
                FROM schedule_place sp
                WHERE sp.schedule_id = se.schedule_id
                  AND sp.place_id IS NOT NULL
            )
            ORDER BY (se.embedding <=> ?) ASC
            LIMIT ?
            """;

        List<SimilarSchedule> result = jdbcTemplate.query(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setObject(1, qvec, Types.OTHER);
            ps.setObject(2, qvec, Types.OTHER);
            ps.setInt(3, limit);
            return ps;
        }, rowMapper);

        return result;
    }

    private PGobject toPgVectorObject(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 12 + 2);
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
