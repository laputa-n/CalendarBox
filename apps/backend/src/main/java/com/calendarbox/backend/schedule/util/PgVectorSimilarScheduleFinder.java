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

        Double self = jdbcTemplate.queryForObject(
                "SELECT embedding <=> embedding FROM schedule_embedding LIMIT 1",
                Double.class
        );
        log.info("[similar][self-dist] {}", self);

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

        log.info("[similar] start findSimilar, limit={}, title={}, memo={}",
                limit, request.title(), request.memo());

        // 1) 검색용 임베딩 생성
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);
        log.info("[similar] embedding dimension = {}, first3 = [{}, {}, {}]",
                queryEmbedding.length,
                queryEmbedding.length > 0 ? queryEmbedding[0] : null,
                queryEmbedding.length > 1 ? queryEmbedding[1] : null,
                queryEmbedding.length > 2 ? queryEmbedding[2] : null
        );

        // 2) float[] -> vector literal string (중요: E-32 같은 과학표기법 방지)
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        log.info("[similar] vector literal prefix = {}", vectorLiteral.substring(0, Math.min(120, vectorLiteral.length())));

        Integer parsed = jdbcTemplate.queryForObject(
                "SELECT 1 WHERE (?::vector) IS NOT NULL",
                Integer.class,
                vectorLiteral
        );
        log.info("[debug] vectorLiteral parse test = {}", parsed);


        List<Long> anchorTop = jdbcTemplate.queryForList("""
    SELECT se.schedule_id
    FROM schedule_embedding se
    WHERE EXISTS (
      SELECT 1 FROM schedule_place sp
      WHERE sp.schedule_id = se.schedule_id
        AND sp.place_id IS NOT NULL
    )
    ORDER BY se.embedding <=> (SELECT embedding FROM schedule_embedding WHERE schedule_id = 5)
    LIMIT 5
""", Long.class);
        log.info("[debug] anchorTop(db-ref) = {}", anchorTop);

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
        log.info("[debug] queryTop(literal) = {}", queryTop);

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

        List<SimilarSchedule> result = jdbcTemplate.query(
                conn -> {
                    var ps = conn.prepareStatement(sql);
                    ps.setString(1, vectorLiteral);
                    ps.setString(2, vectorLiteral);
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
