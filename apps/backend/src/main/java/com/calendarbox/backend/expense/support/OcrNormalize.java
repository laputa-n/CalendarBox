package com.calendarbox.backend.expense.support;

import com.calendarbox.backend.expense.dto.response.NormalizedReceipt;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class OcrNormalize {

    public static NormalizedReceipt normalize(Map<String, Object> raw) {
        NormalizedReceipt v2 = parseV2(raw);
        if (v2 != null && (v2.totalAmount() > 0 || !v2.items().isEmpty())) {
            return v2;
        }
        NormalizedReceipt legacy = parseLegacy(raw);
        if (legacy != null) return legacy;
        return new NormalizedReceipt("영수증", null, 0L, List.of(), raw);
    }

    /* =============== V2 parser =============== */
    private static NormalizedReceipt parseV2(Map<String, Object> raw) {
        try {
            Map<String, Object> img0 = firstMapFromList(raw.get("images"));
            if (img0 == null) return null;

            // V2 실제 루트
            Map<String, Object> result = asMapOrNull(nested(img0, "receipt", "result"));
            if (result == null) return null;

            // 총액
            long total = toLong(nested(result, "totalPrice", "price", "formatted", "value"));

            // 상호명
            String merchant = toStr(nested(result, "storeInfo", "name", "formatted", "value"), null);
            if (isBlank(merchant)) merchant = toStr(nested(result, "storeInfo", "name", "text"), null);

            // fallback: subName도 시도
            if (isBlank(merchant)) {
                merchant = toStr(nested(result, "storeInfo", "subName", "text"), "영수증");
            }

            // 항목들
            List<NormalizedReceipt.Item> items = new ArrayList<>();
            List<?> subResults = asListOrEmpty(result.get("subResults"));
            for (Object srObj : subResults) {
                Map<String, Object> sr = asMapOrNull(srObj);
                if (sr == null) continue;
                for (Object itObj : asListOrEmpty(sr.get("items"))) {
                    Map<String, Object> m = asMapOrNull(itObj);
                    if (m == null) continue;

                    String label = toStr(nested(m, "name", "text"), "항목");
                    int   qty    = toInt(nested(m, "count", "formatted", "value"), 1);
                    long  unit   = toLong(nested(m, "price", "unitPrice", "formatted", "value"));
                    long  line   = toLong(nested(m, "price", "price", "formatted", "value"));

                    if (unit == 0 && qty > 0) unit = line / Math.max(qty, 1);
                    if (isBlank(label) && qty <= 0 && line <= 0) continue;

                    items.add(new NormalizedReceipt.Item(label, qty, unit, line));
                }
            }

            // 총액 보정 (총액 0이면 라인 합계 사용)
            if (total == 0 && !items.isEmpty()) {
                total = items.stream().mapToLong(NormalizedReceipt.Item::lineAmount).sum();
            }

            // 결제 시간
            Map<String, Object> dateFmt = asMapOrNull(nested(result, "paymentInfo", "date", "formatted"));
            Map<String, Object> timeFmt = asMapOrNull(nested(result, "paymentInfo", "time", "formatted"));
            Instant paidAt = null;
            if (dateFmt != null) {
                int y  = toInt(dateFmt.get("year"), 1970);
                int m  = toInt(dateFmt.get("month"), 1);
                int d  = toInt(dateFmt.get("day"), 1);
                int hh = (timeFmt != null) ? toInt(timeFmt.get("hour"),   0) : 0;
                int mm = (timeFmt != null) ? toInt(timeFmt.get("minute"), 0) : 0;
                int ss = (timeFmt != null) ? toInt(timeFmt.get("second"), 0) : 0;
                paidAt = ZonedDateTime.of(y, m, d, hh, mm, ss, 0, ZoneId.of("Asia/Seoul")).toInstant();
            }

            return new NormalizedReceipt(
                    isBlank(merchant) ? "영수증" : merchant,
                    paidAt,
                    total,
                    items,
                    raw
            );
        } catch (Exception e) {
            return null; // legacy로 폴백
        }
    }



    /* =============== Legacy(V1) parser =============== */
    private static NormalizedReceipt parseLegacy(Map<String, Object> raw) {
        try {
            Map<String, Object> img0 = firstMapFromList(raw.get("images"));
            if (img0 == null) return null;

            Map<String, Object> receipt = asMapOrNull(img0.get("receipt"));
            Map<String, Object> result  = asMapOrNull(receipt != null ? receipt.get("result") : null);
            if (result == null) return null;

            Map<String, Object> storeInfo   = asMapOrNull(result.get("storeInfo"));
            Map<String, Object> totalPrice  = asMapOrNull(result.get("totalPrice"));
            Map<String, Object> paymentInfo = asMapOrNull(result.get("paymentInfo"));

            String merchant = toStr(storeInfo != null ? storeInfo.get("name") : null, "영수증");
            long total = toLong(totalPrice != null ? totalPrice.get("price") : null);

            Instant paidAt = null;
            String dateStr = toStr(paymentInfo != null ? paymentInfo.get("date") : null, null);
            if (dateStr != null) {
                try {
                    var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    paidAt = LocalDateTime.parse(dateStr.replace("  ", " ").trim(), fmt)
                            .atZone(ZoneId.systemDefault()).toInstant();
                } catch (Exception ignore) { }
            }

            List<NormalizedReceipt.Item> items = List.of();
            List<?> subResults = asListOrEmpty(result.get("subResults"));
            if (!subResults.isEmpty()) {
                items = subResults.stream()
                        .map(OcrNormalize::asMapOrNull)
                        .filter(Objects::nonNull)
                        .map(sr -> asListOrEmpty(sr.get("items")))
                        .flatMap(List::stream)
                        .map(OcrNormalize::mapLegacyItem)
                        .collect(Collectors.toList());
            }

            return new NormalizedReceipt(merchant, paidAt, total, items, raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static NormalizedReceipt.Item mapLegacyItem(Object obj) {
        Map<String, Object> m = asMapOrNull(obj);
        if (m == null) return new NormalizedReceipt.Item("항목", 1, 0, 0);
        String label = toStr(m.get("name"), "항목");
        int qty      = toInt(m.get("count"), 1);
        long unit    = toLong(m.get("unitPrice"));
        long line    = toLong(m.get("price"));
        if (unit == 0 && qty > 0) unit = line / Math.max(qty, 1);
        return new NormalizedReceipt.Item(label, qty, unit, line);
    }

    /* =============== helpers =============== */
    private static Map<String, Object> asMapOrNull(Object v) {
        return (v instanceof Map<?, ?> m) ? castMap(m) : null;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }
    private static List<?> asListOrEmpty(Object v) {
        return (v instanceof List<?> l) ? l : List.of();
    }
    private static Map<String, Object> firstMapFromList(Object v) {
        if (!(v instanceof List<?> l) || l.isEmpty()) return null;
        Object first;
        // JDK 21: l.getFirst(); 이하 호환 위해 0 사용
        first = l.get(0);
        return asMapOrNull(first);
    }
    private static Object nested(Map<String, Object> m, String... path) {
        Object cur = m;
        for (String k : path) {
            if (!(cur instanceof Map<?, ?> mm)) return null;
            cur = mm.get(k);
            if (cur == null) return null;
        }
        return cur;
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String toStr(Object v, String def) { return v == null ? def : String.valueOf(v).trim(); }
    private static int toInt(Object v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(String.valueOf(v).replaceAll("[^0-9-]", "")); }
        catch (Exception e) { return def; }
    }
    private static long toLong(Object v) {
        if (v == null) return 0L;
        try { return Long.parseLong(String.valueOf(v).replaceAll("[^0-9-]", "")); }
        catch (Exception e) { return 0L; }
    }
}
