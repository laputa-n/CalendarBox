package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
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
        var dbInfo = jdbcTemplate.queryForMap("""
    SELECT
      current_database()   AS db,
      current_user         AS usr,
      inet_server_addr()   AS addr,
      inet_server_port()   AS port,
      current_schema()     AS schema
""");
        log.info("[similar][dbinfo] {}", dbInfo);

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
                queryEmbedding[0], queryEmbedding[1], queryEmbedding[2]
        );

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

        List<SimilarSchedule> result = jdbcTemplate.query(
                conn -> {
                    // 🔑 pgvector 타입 등록
                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    pgConn.addDataType("vector", PGvector.class);

                    var ps = conn.prepareStatement(sql);

                    PGvector vector = new PGvector(queryEmbedding);

                    ps.setObject(1, vector); // similarity 계산
                    ps.setObject(2, vector); // ORDER BY
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
}
