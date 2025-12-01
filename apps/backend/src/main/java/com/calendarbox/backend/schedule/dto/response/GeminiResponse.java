package com.calendarbox.backend.schedule.dto.response;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    public String getText() {
        if (candidates == null || candidates.isEmpty()) return "";
        Candidate c = candidates.get(0);
        if (c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return "";
        Part p = c.content.parts.get(0);
        return p.text == null ? "" : p.text;
    }

    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}

