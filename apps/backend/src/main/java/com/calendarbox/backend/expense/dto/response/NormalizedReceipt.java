package com.calendarbox.backend.expense.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NormalizedReceipt(
        String merchantName, Instant paidAt, Long totalAmount, List<Item> items, Map<String, Object> raw
) {
    public record Item(String label, int qty, long unitAmount, long lineAmount){
        public Map<String,Object> toMap() {
            return Map.of(
                    "label", label,
                    "qty", qty,
                    "unitAmount", unitAmount,
                    "lineAmount", lineAmount
            );
        }
    }

    public Map<String,Object> asMap() {
        var itemMaps = (items == null) ? List.<Map<String,Object>>of()
                : items.stream().map(Item::toMap).toList();
        var m = new java.util.LinkedHashMap<String,Object>();
        m.put("merchantName", getMerchantNameOrDefault("영수증"));
        if (paidAt != null) m.put("paidAt", paidAt.toString());
        m.put("totalAmount", totalAmount == null ? 0L : totalAmount);
        m.put("items", itemMaps);
        return m;
    }
    public String getMerchantNameOrDefault(String def) { return merchantName != null? merchantName : def; }
}

