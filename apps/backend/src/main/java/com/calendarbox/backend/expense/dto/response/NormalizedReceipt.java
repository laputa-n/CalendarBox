package com.calendarbox.backend.expense.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NormalizedReceipt(
        String merchantName, Instant paidAt, Long totalAmount, List<Item> items, Map<String, Object> raw
) {
    public record Item(String label, int qty, long unitAmount, long lineAmount){}
    public Map<String,Object> asMap() {
        return Map.of(
                "merchantName", merchantName,
                "paidAt", paidAt != null ? paidAt.toString() : null,
                "totalAmount", totalAmount,
                "items", items
        );
    }
    public String getMerchantNameOrDefault(String def) { return merchantName != null? merchantName : def; }
}
