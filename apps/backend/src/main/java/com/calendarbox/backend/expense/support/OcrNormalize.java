package com.calendarbox.backend.expense.support;

import com.calendarbox.backend.expense.dto.response.NormalizedReceipt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OcrNormalize {

    public static NormalizedReceipt normalize(Map<String, Object> raw) {
        try {
            var images = (List<Map<String, Object>>) raw.get("images");
            if (images == null || images.isEmpty()) {
                throw new IllegalStateException("OCR 응답에 이미지 데이터가 없습니다.");
            }

            var receipt = (Map<String, Object>) images.get(0).get("receipt");
            if (receipt == null) throw new IllegalStateException("receipt 필드 없음");

            var result = (Map<String, Object>) receipt.get("result");
            if (result == null) throw new IllegalStateException("result 필드 없음");

            var storeInfo = (Map<String, Object>) result.get("storeInfo");
            var totalPrice = (Map<String, Object>) result.get("totalPrice");
            var paymentInfo = (Map<String, Object>) result.get("paymentInfo");
            var subResults = (List<Map<String, Object>>) result.get("subResults");

            String merchantName = Optional.ofNullable(storeInfo)
                    .map(m -> (String) m.get("name"))
                    .orElse("영수증");

            long totalAmount = Optional.ofNullable(totalPrice)
                    .map(t -> Long.parseLong((String) t.get("price")))
                    .orElse(0L);

            String dateStr = Optional.ofNullable(paymentInfo)
                    .map(p -> (String) p.get("date"))
                    .orElse(null);

            Instant paidAt = null;
            if (dateStr != null) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    paidAt = LocalDateTime.parse(dateStr, fmt)
                            .atZone(ZoneId.systemDefault())
                            .toInstant();
                } catch (Exception ignore) { }
            }

            // 세부 항목 (items)
            List<NormalizedReceipt.Item> items = Collections.emptyList();
            if (subResults != null && !subResults.isEmpty()) {
                items = subResults.stream()
                        .map(sr -> (List<Map<String, Object>>) sr.get("items"))
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .map(OcrNormalize::mapToItem)
                        .collect(Collectors.toList());
            }

            return new NormalizedReceipt(merchantName, paidAt, totalAmount, items, raw);


        } catch (Exception e) {
            throw new RuntimeException("OCR 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    private static NormalizedReceipt.Item mapToItem(Map<String, Object> map) {
        String label = (String) map.get("name");
        int qty = parseInt(map.get("count"));
        long unit = parseLong(map.get("unitPrice"));
        long lineAmount = parseLong(map.get("price"));

        if(unit == 0 && qty > 0){
            unit = lineAmount / qty;
        }
        return new NormalizedReceipt.Item(label, qty, unit, lineAmount);
    }


    private static int parseInt(Object v) {
        if (v == null) return 1;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 1; }
    }

    private static long parseLong(Object v) {
        if (v == null) return 0;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0; }
    }
}
