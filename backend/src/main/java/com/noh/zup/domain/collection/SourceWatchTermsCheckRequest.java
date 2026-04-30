package com.noh.zup.domain.collection;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SourceWatchTermsCheckRequest(
        TermsCheckStatus termsCheckStatus,
        @Size(max = 1000) String termsUrl,
        LocalDate termsCheckedAt,
        @Size(max = 2000) String termsMemo
) {
}
