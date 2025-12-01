package com.calendarbox.backend.place.util;

import com.calendarbox.backend.schedule.enums.ScheduleCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPlaceCategoryWeigher implements PlaceCategoryWeigher {

    @Override
    public double weight(ScheduleCategory scheduleCategory, String placeCategory) {
        if (placeCategory == null || placeCategory.isBlank()) return 0.0;

        List<String> keywords = extractKeywords(placeCategory);

        return switch (scheduleCategory) {
            case DINNER, FAMILY -> dinnerWeight(keywords);
            case CAFE, STUDY    -> cafeStudyWeight(keywords);
            case DRINK          -> drinkWeight(keywords);
            case WORKOUT        -> workoutWeight(keywords);
            case TRIP           -> tripWeight(keywords);
            case SHOPPING       -> shoppingWeight(keywords);
            case HOSPITAL       -> hospitalWeight(keywords);
            case BEAUTY         -> beautyWeight(keywords);
            case MEETING, CULTURE, OTHER -> 0.3; // 일단 애매하면 낮은 기본값
        };
    }

    private List<String> extractKeywords(String raw) {
        String[] levels = raw.split(">");
        List<String> tokens = new ArrayList<>();
        for (String level : levels) {
            for (String part : level.split(",")) {
                String t = part.trim().toLowerCase();
                if (!t.isEmpty()) tokens.add(t);
            }
        }
        return tokens;
    }

    private double dinnerWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("음식점") || s.contains("한식") || s.contains("중식") || s.contains("일식")
                || s.contains("양식") || s.contains("고기") || s.contains("치킨") || s.contains("분식")))
            return 1.0;
        if (k.stream().anyMatch(s -> s.contains("카페") || s.contains("디저트")))
            return 0.6;
        return 0.1;
    }

    private double cafeStudyWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("카페") || s.contains("디저트")))
            return 1.0;
        if (k.stream().anyMatch(s -> s.contains("스터디") || s.contains("독서실")))
            return 0.8;
        if (k.stream().anyMatch(s -> s.contains("음식점")))
            return 0.4;
        return 0.1;
    }

    private double drinkWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("술집") || s.contains("호프") || s.contains("포차") || s.contains("바")))
            return 1.0;
        if (k.stream().anyMatch(s -> s.contains("음식점")))
            return 0.5;
        return 0.0;
    }

    private double workoutWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("헬스") || s.contains("체육관")
                || s.contains("필라테스") || s.contains("요가") || s.contains("클라이밍")))
            return 1.0;
        return 0.0;
    }

    private double tripWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("공원") || s.contains("관광") || s.contains("명소")
                || s.contains("테마파크") || s.contains("동물원") || s.contains("식물원")))
            return 1.0;
        if (k.stream().anyMatch(s -> s.contains("카페") || s.contains("음식점")))
            return 0.5;
        return 0.1;
    }

    private double shoppingWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("마트") || s.contains("백화점")
                || s.contains("쇼핑") || s.contains("시장") || s.contains("편의점")))
            return 1.0;
        return 0.1;
    }

    private double hospitalWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("병원") || s.contains("의원")
                || s.contains("치과") || s.contains("한의원") || s.contains("약국")))
            return 1.0;
        return 0.0;
    }

    private double beautyWeight(List<String> k) {
        if (k.stream().anyMatch(s -> s.contains("피부관리") || s.contains("에스테틱")
                || s.contains("미용실") || s.contains("네일") || s.contains("헤어")))
            return 1.0;
        return 0.0;
    }
}
