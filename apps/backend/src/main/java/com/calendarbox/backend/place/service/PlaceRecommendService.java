package com.calendarbox.backend.place.service;

import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import com.calendarbox.backend.place.dto.response.PlacePreview;
import com.calendarbox.backend.place.dto.response.PlaceResponseDto;
import com.calendarbox.backend.place.util.PlaceCategoryWeigher;
import com.calendarbox.backend.schedule.enums.ScheduleCategory;
import com.calendarbox.backend.schedule.repository.SchedulePlaceRepository;
import com.calendarbox.backend.schedule.util.GeminiScheduleCategoryExtractor;
import com.calendarbox.backend.schedule.util.PgVectorSimilarScheduleFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceRecommendService {
    private final PlaceSearchService placeSearchService;
    private final GeminiScheduleCategoryExtractor categoryExtractor;
    private final PlaceCategoryWeigher placeCategoryWeigher;
    private final PgVectorSimilarScheduleFinder similarScheduleFinder;
    private final SchedulePlaceRepository schedulePlaceRepository;


    public List<PlaceResponseDto> recommend(PlaceRecommendRequest request){
        log.info("[recommend] request = {}", request);

        List<PlacePreview> anchorPlaces = placeSearchService.search(request.regionQuery(),5,"random");
        log.info("[recommend] anchorPlaces size = {}", anchorPlaces.size());

        double anchorLng = 0;
        double anchorLat = 0;

        if(!anchorPlaces.isEmpty()){
            for(PlacePreview p:anchorPlaces){
                anchorLng += p.lng();
                anchorLat += p.lat();
            }
            anchorLng /= anchorPlaces.size();
            anchorLat /= anchorPlaces.size();
        }


        final double centerLng = anchorLng;
        final double centerLat = anchorLat;
        log.info("[recommend] centerLat={}, centerLng={}", centerLat, centerLng);

        //일정 카테고리 추출
        ScheduleCategory category = categoryExtractor.extract(request);
        log.info("[recommend] extracted category = {}", category);

        var similarList = similarScheduleFinder.findSimilar(request,50);
        log.info("[recommend] similarList size = {}", similarList.size());
        log.info("[recommend] similarList detail = {}", similarList);

        List<Long> scheduleIds = similarList.stream()
                .map(PgVectorSimilarScheduleFinder.SimilarSchedule::scheduleId).toList();

        if (scheduleIds.isEmpty()) {
            log.warn("[recommend] no similar schedules found, returning empty result");
            return List.of();
        }

        var similarityMap = similarList.stream()
                .collect(Collectors.toMap(
                        PgVectorSimilarScheduleFinder.SimilarSchedule::scheduleId,
                        PgVectorSimilarScheduleFinder.SimilarSchedule::similarity
                ));

        log.info("[recommend] similarityMap keys(scheduleIds) = {}", similarityMap.keySet());

        List<SchedulePlaceRepository.SchedulePlaceProjection> rows =
                schedulePlaceRepository.findByScheduleIds(scheduleIds);
        log.info("[recommend] schedule places rows size = {}", rows.size());

        var placeMaxSimMap = new HashMap<Place,Double>();

        for(var row:rows){
            Place place = row.getPlace();
            double sim = similarityMap.getOrDefault(row.getScheduleId(),0.0);

            placeMaxSimMap.merge(place,sim,Math::max);
        }

        record ScoredPlace(Place place, double score) {}

        List<ScoredPlace> scored = placeMaxSimMap.entrySet().stream()
                .map(entry -> {
                    Place place = entry.getKey();
                    double sim = entry.getValue(); // 0~1 가정

                    double catWeight = placeCategoryWeigher.weight(
                            category,
                            place.getCategory()   // 네이버 카테고리 문자열
                    );

                    double distanceKm = haversine(centerLat, centerLng,
                            place.getLat().doubleValue(), place.getLng().doubleValue());
                    double distanceScore = 1.0 / (1.0 + distanceKm); // 가까울수록 1에 가까움 (0~1 근처)

                    // 7) 가중합: 필요에 따라 비율 조절
                    double score =
                            0.5 * sim +          // 유사도 비중
                                    0.3 * catWeight +    // 카테고리 가중치
                                    0.2 * distanceScore; // 거리 점수

                    return new ScoredPlace(place, score);
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(10)
                .toList();


        return scored.stream()
                .map(sp -> PlaceResponseDto.from(sp.place()))
                .toList();
    }
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}
