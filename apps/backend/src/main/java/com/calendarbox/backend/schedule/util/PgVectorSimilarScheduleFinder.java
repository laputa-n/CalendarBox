package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSimilarScheduleFinder {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;

    public record SimilarSchedule(Long scheduleId, double similarity) {}

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        log.info("[similar] start findSimilar, limit={}, title={}, memo={}",
                limit, request.title(), request.memo());

        Integer totalEmb = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM schedule_embedding",
                Integer.class
        );

        Integer withPlace = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM schedule_embedding se
                WHERE EXISTS (
                    SELECT 1
                    FROM schedule_place sp
                    WHERE sp.schedule_id = se.schedule_id
                      AND sp.place_id IS NOT NULL
                )
                """,
                Integer.class
        );

        log.info("[similar][debug] from JDBC: totalEmb={}, withPlace={}", totalEmb, withPlace);

        // 1) 요청 텍스트 -> 임베딩
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);
        log.info("[similar] embedding dimension = {}, first3 = [{}, {}, {}]",
                queryEmbedding.length,
                queryEmbedding.length > 0 ? queryEmbedding[0] : null,
                queryEmbedding.length > 1 ? queryEmbedding[1] : null,
                queryEmbedding.length > 2 ? queryEmbedding[2] : null
        );

        // 2) vector literal 만들기: "[0.1,0.2,...]"
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        log.info("[similar] vector literal prefix = {}",
                vectorLiteral.substring(0, Math.min(120, vectorLiteral.length())));

        // 3) SQL: 캐스팅 강제 + 거리식으로 정렬(가장 안전)
        String sql = """
            SELECT se.schedule_id,
                   1 - (se.embedding <=> (?::vector)) AS similarity
            FROM schedule_embedding se
            WHERE EXISTS (
                SELECT 1
                FROM schedule_place sp
                WHERE sp.schedule_id = se.schedule_id
                  AND sp.place_id IS NOT NULL
            )
            ORDER BY (se.embedding <=> (?::vector)) ASC
            LIMIT ?
            """;

        log.debug("[similar] SQL = {}", sql);

        List<SimilarSchedule> result = jdbcTemplate.query(
                connection -> {
                    var ps = connection.prepareStatement(sql);
                    // 파라미터 1: similarity 계산용
                    ps.setString(1, vectorLiteral);
                    // 파라미터 2: ORDER BY 거리 계산용
                    ps.setString(2, vectorLiteral);
                    // 파라미터 3: limit
                    ps.setInt(3, limit);
                    return ps;
                },
                (rs, rowNum) -> new SimilarSchedule(
                        rs.getLong("schedule_id"),
                        rs.getDouble("similarity")
                )
        );

        log.info("[similar] result size = {}", result.size());
        log.info("[similar] scheduleIds = {}", result.stream().map(SimilarSchedule::scheduleId).toList());
        log.info("[similar] similarities = {}", result.stream().map(SimilarSchedule::similarity).toList());

        return result;
    }
    
    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
