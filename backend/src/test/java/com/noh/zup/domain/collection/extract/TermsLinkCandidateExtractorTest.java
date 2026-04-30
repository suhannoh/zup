package com.noh.zup.domain.collection.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.noh.zup.domain.collection.TermsLinkCandidateConfidence;
import com.noh.zup.domain.collection.TermsLinkCandidateType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermsLinkCandidateExtractorTest {

    private final TermsLinkCandidateExtractor extractor = new TermsLinkCandidateExtractor();

    @Test
    void extractsTermsCandidatesWithAbsoluteUrlsAndDeduplication() {
        String html = """
                <html><body>
                  <a href="/terms">이용약관</a>
                  <a href="https://example.com/terms">Terms of Service</a>
                  <a href="/privacy">개인정보처리방침</a>
                  <a href="/legal">Legal</a>
                  <a href="/support">고객센터</a>
                </body></html>
                """;

        List<com.noh.zup.domain.collection.TermsLinkCandidate> candidates = extractor.extract(
                html,
                "https://example.com/events/page"
        );

        assertThat(candidates)
                .extracting("url")
                .containsExactly(
                        "https://example.com/terms",
                        "https://example.com/privacy",
                        "https://example.com/legal",
                        "https://example.com/support"
                );
        assertThat(candidates.get(0).type()).isEqualTo(TermsLinkCandidateType.TERMS);
        assertThat(candidates.get(0).confidence()).isEqualTo(TermsLinkCandidateConfidence.HIGH);
        assertThat(candidates.get(1).type()).isEqualTo(TermsLinkCandidateType.PRIVACY);
        assertThat(candidates.get(3).type()).isEqualTo(TermsLinkCandidateType.OTHER);
    }
}
