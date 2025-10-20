package com.calendarbox.backend.global.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,                  // 0-based
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast(),
                p.hasNext(),
                p.hasPrevious()
        );
    }

    public static <S,T> PageResponse<T> map(Page<S> p, java.util.function.Function<S,T> f) {
        var content = p.getContent().stream().map(f).toList();
        return new PageResponse<>(
                content,
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast(),
                p.hasNext(),
                p.hasPrevious()
        );
    }
}
