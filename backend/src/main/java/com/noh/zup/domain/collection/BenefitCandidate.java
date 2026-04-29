package com.noh.zup.domain.collection;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.brand.Brand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "benefit_candidates",
        indexes = {
                @Index(name = "idx_benefit_candidates_collection_run_id", columnList = "collection_run_id")
        }
)
public class BenefitCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_watch_id", nullable = false)
    private SourceWatch sourceWatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PageSnapshot snapshot;

    @Column(name = "collection_run_id")
    private Long collectionRunId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BenefitType benefitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OccasionType occasionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BirthdayTimingType birthdayTimingType;

    @Column(nullable = false)
    private Boolean requiresApp;

    @Column(nullable = false)
    private Boolean requiresSignup;

    @Column(nullable = false)
    private Boolean requiresMembership;

    @Lob
    @Column(nullable = false)
    private String evidenceText;

    @Lob
    private String benefitDetailText;

    @Lob
    private String benefitDetailImageSources;

    @Lob
    private String usageGuideText;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BenefitCandidateStatus status = BenefitCandidateStatus.NEEDS_REVIEW;

    @Lob
    private String reviewMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_benefit_id")
    private Benefit approvedBenefit;

    private LocalDateTime approvedAt;

    protected BenefitCandidate() {
    }

    public BenefitCandidate(
            Brand brand,
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            String title,
            String summary,
            BenefitType benefitType,
            OccasionType occasionType,
            BirthdayTimingType birthdayTimingType,
            Boolean requiresApp,
            Boolean requiresSignup,
            Boolean requiresMembership,
            String evidenceText,
            String benefitDetailText,
            String benefitDetailImageSources,
            String usageGuideText,
            BigDecimal confidence
    ) {
        this(
                brand,
                sourceWatch,
                snapshot,
                null,
                title,
                summary,
                benefitType,
                occasionType,
                birthdayTimingType,
                requiresApp,
                requiresSignup,
                requiresMembership,
                evidenceText,
                benefitDetailText,
                benefitDetailImageSources,
                usageGuideText,
                confidence
        );
    }

    public BenefitCandidate(
            Brand brand,
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            String title,
            String summary,
            BenefitType benefitType,
            OccasionType occasionType,
            BirthdayTimingType birthdayTimingType,
            Boolean requiresApp,
            Boolean requiresSignup,
            Boolean requiresMembership,
            String evidenceText,
            BigDecimal confidence
    ) {
        this(
                brand,
                sourceWatch,
                snapshot,
                null,
                title,
                summary,
                benefitType,
                occasionType,
                birthdayTimingType,
                requiresApp,
                requiresSignup,
                requiresMembership,
                evidenceText,
                confidence
        );
    }

    public BenefitCandidate(
            Brand brand,
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            Long collectionRunId,
            String title,
            String summary,
            BenefitType benefitType,
            OccasionType occasionType,
            BirthdayTimingType birthdayTimingType,
            Boolean requiresApp,
            Boolean requiresSignup,
            Boolean requiresMembership,
            String evidenceText,
            String benefitDetailText,
            String benefitDetailImageSources,
            String usageGuideText,
            BigDecimal confidence
    ) {
        this.brand = brand;
        this.sourceWatch = sourceWatch;
        this.snapshot = snapshot;
        this.collectionRunId = collectionRunId;
        this.title = title;
        this.summary = summary;
        this.benefitType = benefitType;
        this.occasionType = occasionType;
        this.birthdayTimingType = birthdayTimingType;
        this.requiresApp = requiresApp;
        this.requiresSignup = requiresSignup;
        this.requiresMembership = requiresMembership;
        this.evidenceText = evidenceText;
        this.benefitDetailText = benefitDetailText;
        this.benefitDetailImageSources = benefitDetailImageSources;
        this.usageGuideText = usageGuideText;
        this.confidence = confidence;
        this.status = BenefitCandidateStatus.NEEDS_REVIEW;
    }

    public BenefitCandidate(
            Brand brand,
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            Long collectionRunId,
            String title,
            String summary,
            BenefitType benefitType,
            OccasionType occasionType,
            BirthdayTimingType birthdayTimingType,
            Boolean requiresApp,
            Boolean requiresSignup,
            Boolean requiresMembership,
            String evidenceText,
            BigDecimal confidence
    ) {
        this(
                brand,
                sourceWatch,
                snapshot,
                collectionRunId,
                title,
                summary,
                benefitType,
                occasionType,
                birthdayTimingType,
                requiresApp,
                requiresSignup,
                requiresMembership,
                evidenceText,
                null,
                null,
                null,
                confidence
        );
    }

    public void updateStatus(BenefitCandidateStatus status, String reviewMemo) {
        this.status = status;
        if (reviewMemo != null) {
            this.reviewMemo = reviewMemo;
        }
    }

    public void approve(Benefit approvedBenefit, String reviewMemo) {
        this.status = BenefitCandidateStatus.APPROVED;
        this.approvedBenefit = approvedBenefit;
        this.approvedAt = LocalDateTime.now();
        if (reviewMemo != null) {
            this.reviewMemo = reviewMemo;
        }
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public SourceWatch getSourceWatch() {
        return sourceWatch;
    }

    public PageSnapshot getSnapshot() {
        return snapshot;
    }

    public Long getCollectionRunId() {
        return collectionRunId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public BenefitType getBenefitType() {
        return benefitType;
    }

    public OccasionType getOccasionType() {
        return occasionType;
    }

    public BirthdayTimingType getBirthdayTimingType() {
        return birthdayTimingType;
    }

    public Boolean getRequiresApp() {
        return requiresApp;
    }

    public Boolean getRequiresSignup() {
        return requiresSignup;
    }

    public Boolean getRequiresMembership() {
        return requiresMembership;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public String getBenefitDetailText() {
        return benefitDetailText;
    }

    public String getBenefitDetailImageSources() {
        return benefitDetailImageSources;
    }

    public String getUsageGuideText() {
        return usageGuideText;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public BenefitCandidateStatus getStatus() {
        return status;
    }

    public String getReviewMemo() {
        return reviewMemo;
    }

    public Benefit getApprovedBenefit() {
        return approvedBenefit;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
}
