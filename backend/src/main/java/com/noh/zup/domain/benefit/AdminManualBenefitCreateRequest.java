package com.noh.zup.domain.benefit;

import com.noh.zup.domain.collection.CollectionMethod;
import com.noh.zup.domain.source.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AdminManualBenefitCreateRequest(
        @NotNull Long brandId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 500) String summary,
        String detail,
        @NotNull BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        @Size(max = 500) String conditionSummary,
        Boolean requiredApp,
        Boolean requiredSignup,
        Boolean requiredMembership,
        Boolean requiredPurchase,
        @Size(max = 100) String membershipGrade,
        @Size(max = 500) String usagePeriodDescription,
        LocalDate availableFrom,
        LocalDate availableTo,
        String caution,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        Boolean isActive,
        List<@Valid ManualBenefitDetailItemRequest> detailItems,
        @NotEmpty List<@Valid ManualBenefitSourceRequest> sources
) {
    public record ManualBenefitDetailItemRequest(
            @Size(max = 120) String brandName,
            @NotBlank @Size(max = 300) String title,
            @Size(max = 500) String description,
            @Size(max = 500) String conditionText,
            @Size(max = 1000) String imageUrl,
            Integer displayOrder,
            Boolean isActive
    ) {
    }

    public record ManualBenefitSourceRequest(
            @NotNull SourceType sourceType,
            @NotBlank @Size(max = 1000) String sourceUrl,
            @Size(max = 300) String sourceTitle,
            @NotNull LocalDate sourceCheckedAt,
            String memo,
            CollectionMethod collectionMethod,
            String verificationSummary,
            String sourceNotice
    ) {
    }
}
