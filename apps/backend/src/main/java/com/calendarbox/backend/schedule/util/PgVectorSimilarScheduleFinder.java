package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
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

        // 2) float[] -> vector literal string
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        log.info("[similar] vector literal prefix = {}",
                vectorLiteral.substring(0, Math.min(120, vectorLiteral.length())));

        // 3) ✅ 핵심: PGobject(type="vector")로 바인딩 (가장 안정적)
        PGobject vecParam = new PGobject();
        try {
            vecParam.setType("vector");
            vecParam.setValue(vectorLiteral);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build PGobject(vector)", e);
        }

        // (선택) sanity check: 파라미터로 거리 계산이 되는지
        Double qdist = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement("""
                SELECT embedding <=> ? AS dist
                FROM schedule_embedding
                WHERE schedule_id = 5
            """);
            ps.setObject(1, vecParam);
            return ps;
        }, rs -> rs.next() ? rs.getDouble("dist") : null);

        log.info("[debug] dist_to_id5 = {}", qdist);

        // (선택) sanity check: ORDER BY가 이제 정상으로 5개 나오는지
        List<Long> queryTopNoExists = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement("""
                SELECT se.schedule_id
                FROM schedule_embedding se
                ORDER BY se.embedding <=> ?
                LIMIT 5
            """);
            ps.setObject(1, vecParam);
            return ps;
        }, (rs, rowNum) -> rs.getLong(1));

        log.info("[debug] queryTopNoExists(PGobject) = {}", queryTopNoExists);

        // 4) 실제 similar query
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

        List<SimilarSchedule> result = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setObject(1, vecParam);
            ps.setObject(2, vecParam);
            ps.setInt(3, limit);
            return ps;
        }, (rs, rowNum) -> new SimilarSchedule(
                rs.getLong("schedule_id"),
                rs.getDouble("similarity")
        ));

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
