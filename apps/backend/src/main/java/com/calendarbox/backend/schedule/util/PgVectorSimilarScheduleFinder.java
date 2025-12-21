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

    private final RowMapper<SimilarSchedule> rowMapper =
            (rs, rowNum) -> new SimilarSchedule(
                    rs.getLong("schedule_id"),
                    rs.getDouble("similarity")
            );

    public List<SimilarSchedule> findSimilar(PlaceRecommendRequest request, int limit) {
        log.info("[similar] start findSimilar, limit={}, title={}, memo={}",
                limit, request.title(), request.memo());

        // 1) 검색용 임베딩 생성
        float[] queryEmbedding = scheduleEmbeddingService.embedScheduleForSearch(request);
        log.info("[similar] embedding dimension={}, first3=[{}, {}, {}]",
                queryEmbedding.length,
                queryEmbedding.length > 0 ? queryEmbedding[0] : null,
                queryEmbedding.length > 1 ? queryEmbedding[1] : null,
                queryEmbedding.length > 2 ? queryEmbedding[2] : null
        );

        // 2) query vector도 INSERT 때처럼 PGobject(vector)로 보냄
        PGobject qvec = toPgVectorObject(queryEmbedding);
        log.info("[similar] qvec.type={}, prefix={}",
                qvec.getType(),
                qvec.getValue().substring(0, Math.min(120, qvec.getValue().length()))
        );

        // ✅ 이 한 줄이 “JDBC 파라미터가 vector로 먹었는지” 결론 내줌
        Integer notNullCnt = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM schedule_embedding se WHERE (se.embedding <=> ?) IS NOT NULL",
                Integer.class,
                qvec
        );
        log.info("[similar][debug] count(dist is not null) = {}", notNullCnt);

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

        log.info("[similar] result size={}", result.size());
        log.info("[similar] scheduleIds={}", result.stream().map(SimilarSchedule::scheduleId).toList());
        log.info("[similar] similarities={}", result.stream().map(SimilarSchedule::similarity).toList());

        return result;
    }

    private PGobject toPgVectorObject(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 12 + 2);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            // float를 그대로 붙여도 되고, 네가 예전에 BigDecimal로 과학표기 막았던 버전 써도 됨
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
