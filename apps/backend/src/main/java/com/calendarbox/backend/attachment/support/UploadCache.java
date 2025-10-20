package com.calendarbox.backend.attachment.support;

import com.calendarbox.backend.attachment.dto.request.PresignRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UploadCache {
    private final Map<String, PresignRequest> map = new ConcurrentHashMap<>();
    public String put(PresignRequest r) { var id = UUID.randomUUID().toString(); map.put(id, r); return id; }
    public PresignRequest get(String id) { return map.get(id); }
    public void remove(String id) { map.remove(id); }
}

