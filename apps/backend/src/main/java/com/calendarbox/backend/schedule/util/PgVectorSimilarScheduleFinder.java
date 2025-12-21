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

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSimilarScheduleFinder {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;

    public record SimilarSchedule(Long scheduleId, double similarity) {}

    private final RowMapper<SimilarSchedule> similarMapper =
            (rs, rowNum) -> new SimilarSchedule(
                    rs.getLong("schedule_id"),
                    rs.getDouble("similarity")
            );

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        // ---- DB sanity (너가 이미 넣은 로그 유지해도 됨) ----
        log.info("[similar] start findSimilar, limit={}, title={}, memo={}",
                limit, request.title(), request.memo());

        // 1) 검색용 임베딩 생성
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);

        // 2) float[] -> PGobject(vector) (INSERT랑 똑같은 방식)
        PGobject queryVector = toPgVectorObject(queryEmbedding);

        // 3) 핵심: SQL에서 cast(? as vector), ?::vector 전부 제거
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
            ps.setObject(1, queryVector, Types.OTHER);
            ps.setObject(2, queryVector, Types.OTHER);
            ps.setInt(3, limit);
            return ps;
        }, similarMapper);

        log.info("[similar] result size = {}", result.size());
        log.info("[similar] scheduleIds = {}", result.stream().map(SimilarSchedule::scheduleId).toList());
        log.info("[similar] similarities = {}", result.stream().map(SimilarSchedule::similarity).toList());

        return result;
    }

    private PGobject toPgVectorObject(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            // 여기서는 Float.toString 그대로 써도 됨 (원하면 BigDecimal로 바꿔도 OK)
            sb.append(Float.toString(embedding[i]));
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
