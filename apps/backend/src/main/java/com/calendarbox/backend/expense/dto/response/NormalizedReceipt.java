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
        return Map.of(
                "merchantName", getMerchantNameOrDefault("영수증"),
                "paidAt", paidAt != null ? paidAt.toString() : null,
                "totalAmount", totalAmount,
                "items", itemMaps
        );
    }
    public String getMerchantNameOrDefault(String def) { return merchantName != null? merchantName : def; }
}

