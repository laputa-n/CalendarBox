package com.calendarbox.backend.place.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import com.calendarbox.backend.place.dto.response.PlacePreview;
import com.calendarbox.backend.place.dto.response.PlaceResponseDto;
import com.calendarbox.backend.place.service.PlaceRecommendService;
import com.calendarbox.backend.place.service.PlaceSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceSearchService placeSearchService;
    private final MemberRepository memberRepository;
    private final PlaceRecommendService placeRecommendService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "random") String sort
    ) {
        if(!memberRepository.existsById(userId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (query == null || query.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (limit < 1 || limit > 5)           throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (!sort.equals("random") && !sort.equals("comment"))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);

        var items = placeSearchService.search(query.trim(), limit, sort);
        var data = Map.of(
                "total", items.size(),
                "content", items
        );
        return ResponseEntity.ok(ApiResponse.ok("장소 검색 성공", data));
    }

    @PostMapping("/recommendation")
    public ResponseEntity<ApiResponse<List<PlaceResponseDto>>> recommend(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestBody @Valid PlaceRecommendRequest request
    ){
        if(!memberRepository.existsById(userId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        var data = placeRecommendService.recommend(request);

        return ResponseEntity.ok(ApiResponse.ok("장소 추천 성공", data));
    }
}
