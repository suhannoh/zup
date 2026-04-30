package com.noh.zup.domain.collection;

public record TermsLinkCandidate(
        String label,
        String url,
        TermsLinkCandidateType type,
        TermsLinkCandidateConfidence confidence
) {
}
