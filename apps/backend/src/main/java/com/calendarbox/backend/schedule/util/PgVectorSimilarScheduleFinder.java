package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSimilarScheduleFinder {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;

    public record SimilarSchedule(Long scheduleId, double similarity) {}

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        Integer embCount = jdbcTemplate.queryForObject("SELECT count(*) FROM schedule_embedding", Integer.class);
        Integer withPlace = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM schedule_embedding se
            WHERE EXISTS (
              SELECT 1
              FROM schedule_place sp
              WHERE sp.schedule_id = se.schedule_id
                AND sp.place_id IS NOT NULL
            )
        """, Integer.class);
        log.info("[similar][baseline] embCount={}, withPlace={}", embCount, withPlace);

        // 1) 검색용 임베딩 생성
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        log.info("[similar] embedding dimension = {}, prefix = {}",
                queryEmbedding.length,
                vectorLiteral.substring(0, Math.min(120, vectorLiteral.length()))
        );

        // 2) 파싱 체크
        Integer parsed = jdbcTemplate.queryForObject("SELECT 1 WHERE (?::vector) IS NOT NULL", Integer.class, vectorLiteral);
        log.info("[debug] vectorLiteral parse test = {}", parsed);

        // 3) sanity: 테이블 row 확인
        List<Long> plainTop = jdbcTemplate.queryForList("SELECT schedule_id FROM schedule_embedding LIMIT 5", Long.class);
        log.info("[debug] plainTop = {}", plainTop);

        // 4) ★ 핵심: SELECT에서도 ?::vector 로 강제 캐스팅 (PGobject/Types.OTHER 금지)
        List<Long> queryTop = jdbcTemplate.queryForList("""
            SELECT se.schedule_id
            FROM schedule_embedding se
            WHERE EXISTS (
              SELECT 1 FROM schedule_place sp
              WHERE sp.schedule_id = se.schedule_id
                AND sp.place_id IS NOT NULL
            )
            ORDER BY se.embedding <=> (?::vector)
            LIMIT 5
        """, Long.class, vectorLiteral);

        log.info("[debug] queryTop(?::vector) = {}", queryTop);

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
            ORDER BY se.embedding <=> (?::vector) ASC
            LIMIT ?
        """;

        List<SimilarSchedule> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SimilarSchedule(
                        rs.getLong("schedule_id"),
                        rs.getDouble("similarity")
                ),
                vectorLiteral,
                vectorLiteral,
                limit
        );

        log.info("[similar] result size = {}", result.size());
        log.info("[similar] scheduleIds = {}", result.stream().map(SimilarSchedule::scheduleId).toList());
        log.info("[similar] similarities = {}", result.stream().map(SimilarSchedule::similarity).toList());

        return result;
    }

    // float -> vector literal, 과학표기 방지
    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 12 + 2);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(new BigDecimal(Float.toString(embedding[i])).toPlainString());
        }
        sb.append(']');
        return sb.toString();
    }
}
