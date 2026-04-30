package com.noh.zup.domain.collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

final class TermsLinkCandidateJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TermsLinkCandidate>> TYPE = new TypeReference<>() {
    };

    private TermsLinkCandidateJson() {
    }

    static String write(List<TermsLinkCandidate> candidates) {
        try {
            return OBJECT_MAPPER.writeValueAsString(candidates == null ? List.of() : candidates);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize terms link candidates", exception);
        }
    }

    static List<TermsLinkCandidate> read(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
