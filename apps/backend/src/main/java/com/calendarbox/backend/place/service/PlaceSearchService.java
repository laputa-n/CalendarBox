package com.calendarbox.backend.place.service;

import com.calendarbox.backend.place.client.NaverLocalClient;
import com.calendarbox.backend.place.dto.response.NaverLocalResponse;
import com.calendarbox.backend.place.dto.response.PlacePreview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {
    private final NaverLocalClient naver;

    public List<PlacePreview> search(String keyword, int limit, String sort) {
        String q = keyword.trim();
        var items = naver.search(q, Math.min(Math.max(limit,1),5), sort); // 방어 캡핑
        if (items == null || items.isEmpty()) return List.of();

        return items.stream().map(this::toPreview).toList();
    }

    private PlacePreview toPreview(NaverLocalResponse.Item i) {
        String title = i.title().replaceAll("<.*?>", "");
        double lng = toCoord(i.mapx());  // 경도
        double lat = toCoord(i.mapy());  // 위도

        String keyBase = title + "|" + java.util.Objects.toString(i.roadAddress(), i.address());
        String providerPlaceKey = sha256Hex(keyBase.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return new PlacePreview("NAVER", providerPlaceKey, title, i.link(), i.category(),
                i.description(), i.address(), i.roadAddress(), lat, lng);
    }
    private static double toCoord(String v) {
        if (v == null || v.isBlank()) return 0d;
        return Long.parseLong(v) / 1e7d;
    }
}

