package com.calendarbox.backend.member.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.member.dto.response.MemberSearchItem;
import com.calendarbox.backend.member.service.MemberQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberQueryService memberQueryService;

    @Operation(
            summary = "회원 검색",
            description = "이메일/이름/핸드폰번호로 회원을 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MemberSearchItem>>> searchMembers(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @RequestParam("q")String query,
            @PageableDefault(size = 20) Pageable pageable
    ){
        var data = memberQueryService.search(userId, query, pageable);
        return ResponseEntity.ok(ApiResponse.ok("멤버 검색 성공",data));
    }
}
