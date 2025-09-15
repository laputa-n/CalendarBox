package com.calendarbox.backend.global.infra.storage;


import java.util.Collection;

public interface StorageClient {
    String presignPut(String key, String contentType, long size);
    String presignGet(String key, String downloadName, boolean inline);
    void assertExists(String key);
    String toThumbKey(String originalKey);
    void deleteQuietly(String key);
    default void deleteQuietlyAll(Collection<String> keys) {
        if (keys == null) return;
        for (String k : keys) if (k != null && !k.isBlank()) deleteQuietly(k);
    }
}
