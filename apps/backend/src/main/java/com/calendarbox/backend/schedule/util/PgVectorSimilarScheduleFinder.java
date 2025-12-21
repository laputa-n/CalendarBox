package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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

    private final RowMapper<SimilarSchedule> similarMapper =
            (rs, rowNum) -> new SimilarSchedule(
                    rs.getLong("schedule_id"),
                    rs.getDouble("similarity")
            );

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {

        // --- sanity checks ---
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

        // 1) embedding
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);
        log.info("[similar] embedding dimension = {}, first3 = [{}, {}, {}]",
                queryEmbedding.length,
                queryEmbedding.length > 0 ? queryEmbedding[0] : null,
                queryEmbedding.length > 1 ? queryEmbedding[1] : null,
                queryEmbedding.length > 2 ? queryEmbedding[2] : null
        );

        // 2) float[] -> vector literal (scientific notation 방지)
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        log.info("[similar] vector literal prefix = {}",
                vectorLiteral.substring(0, Math.min(120, vectorLiteral.length()))
        );

        // 3) parse test (DB가 vector로 잘 읽는지)
        Integer parsed = jdbcTemplate.queryForObject(
                "SELECT 1 WHERE (CAST(? AS text)::vector) IS NOT NULL",
                Integer.class,
                vectorLiteral
        );
        log.info("[debug] vectorLiteral parse test = {}", parsed);

        // 4) dist to a known vector row (id=5 기준, 네가 이미 확인한 그거)
        Double qdist = jdbcTemplate.query(conn -> {
            var ps = conn.prepareStatement(
                    "SELECT (embedding <=> (CAST(? AS text)::vector)) FROM schedule_embedding WHERE schedule_id = 5"
            );
            ps.setString(1, vectorLiteral);
            return ps;
        }, rs -> rs.next() ? rs.getDouble(1) : null);
        log.info("[debug] dist_to_id5 = {}", qdist);

        // 5) anchorTop: DB 내부 vector vs vector 비교(정상 레퍼런스)
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

        // 6) queryTop: literal을 cast해서 비교(여기가 비면 JDBC/캐스팅 문제가 남아있는 것)
        List<Long> queryTop = jdbcTemplate.query(conn -> {
            var ps = conn.prepareStatement("""
                SELECT se.schedule_id
                FROM schedule_embedding se
                WHERE EXISTS (
                  SELECT 1 FROM schedule_place sp
                  WHERE sp.schedule_id = se.schedule_id
                    AND sp.place_id IS NOT NULL
                )
                ORDER BY se.embedding <=> (CAST(? AS text)::vector)
                LIMIT 5
                """);
            ps.setString(1, vectorLiteral);
            return ps;
        }, (rs, rowNum) -> rs.getLong(1));
        log.info("[debug] queryTop(literal) = {}", queryTop);

        List<Long> queryTopNoExists = jdbcTemplate.query(conn -> {
            var ps = conn.prepareStatement("""
        SELECT se.schedule_id
        FROM schedule_embedding se
        ORDER BY se.embedding <=> (CAST(? AS text)::vector)
        LIMIT 5
    """);
            ps.setString(1, vectorLiteral);
            return ps;
        }, (rs, rowNum) -> rs.getLong(1));

        log.info("[debug] queryTopNoExists(literal) = {}", queryTopNoExists);

        Integer countNoExists = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM schedule_embedding se WHERE (se.embedding <=> (CAST(? AS text)::vector)) IS NOT NULL",
                Integer.class,
                vectorLiteral
        );
        log.info("[debug] countNoExists(dist not null) = {}", countNoExists);

        // 7) main query
        String sql = """
            SELECT se.schedule_id,
                   1 - (se.embedding <=> (CAST(? AS text)::vector)) AS similarity
            FROM schedule_embedding se
            WHERE EXISTS (
                SELECT 1
                FROM schedule_place sp
                WHERE sp.schedule_id = se.schedule_id
                  AND sp.place_id IS NOT NULL
            )
            ORDER BY (se.embedding <=> (CAST(? AS text)::vector)) ASC
            LIMIT ?
            """;

        List<SimilarSchedule> result = jdbcTemplate.query(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, vectorLiteral);
            ps.setString(2, vectorLiteral);
            ps.setInt(3, limit);
            return ps;
        }, similarMapper);

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
            // 과학 표기법 방지 (E-xx 등)
            sb.append(new BigDecimal(Float.toString(embedding[i])).toPlainString());
        }
        sb.append(']');
        return sb.toString();
    }
}
