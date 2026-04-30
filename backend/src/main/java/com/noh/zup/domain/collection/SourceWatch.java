package com.noh.zup.domain.collection;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.source.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "source_watches")
public class SourceWatch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SourceType sourceType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean loginRequired = false;

    private LocalDateTime lastFetchedAt;

    @Column(length = 64)
    private String lastContentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceWatchStatus lastStatus = SourceWatchStatus.READY;

    @Column(nullable = false)
    private Integer failureCount = 0;

    private LocalDateTime nextFetchAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CollectionPermissionStatus collectionPermissionStatus = CollectionPermissionStatus.UNKNOWN_NEEDS_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RobotsCheckStatus robotsCheckStatus = RobotsCheckStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TermsCheckStatus termsCheckStatus = TermsCheckStatus.NOT_CHECKED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionMethod collectionMethod = CollectionMethod.UNKNOWN;

    private LocalDateTime lastPolicyCheckedAt;

    private LocalDateTime lastManualVerifiedAt;

    @Column(columnDefinition = "text")
    private String policyCheckNote;

    @Column(columnDefinition = "text")
    private String manualVerificationNote;

    @Column(length = 1000)
    private String termsUrl;

    private LocalDate termsCheckedAt;

    @Column(columnDefinition = "text")
    private String termsMemo;

    @Column(columnDefinition = "text")
    private String termsLinkCandidatesJson;

    protected SourceWatch() {
    }

    public SourceWatch(Brand brand, SourceType sourceType, String title, String url, Boolean isActive) {
        this.brand = brand;
        this.sourceType = sourceType;
        this.title = title;
        this.url = url;
        this.isActive = isActive != null ? isActive : true;
        this.lastStatus = SourceWatchStatus.READY;
    }

    public void update(SourceType sourceType, String title, String url, Boolean isActive) {
        if (sourceType != null) {
            this.sourceType = sourceType;
        }
        if (title != null) {
            this.title = title;
        }
        if (url != null) {
            this.url = url;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void updatePolicy(
            RobotsCheckStatus robotsCheckStatus,
            TermsCheckStatus termsCheckStatus,
            CollectionMethod collectionMethod,
            Boolean loginRequired,
            CollectionPermissionStatus manualPermissionStatus,
            String policyCheckNote,
            LocalDateTime lastPolicyCheckedAt,
            String manualVerificationNote,
            LocalDateTime lastManualVerifiedAt
    ) {
        if (robotsCheckStatus != null) {
            this.robotsCheckStatus = robotsCheckStatus;
        }
        if (termsCheckStatus != null) {
            this.termsCheckStatus = termsCheckStatus;
        }
        if (collectionMethod != null) {
            this.collectionMethod = collectionMethod;
        }
        if (loginRequired != null) {
            this.loginRequired = loginRequired;
        }
        if (policyCheckNote != null) {
            this.policyCheckNote = policyCheckNote;
        }
        if (lastPolicyCheckedAt != null) {
            this.lastPolicyCheckedAt = lastPolicyCheckedAt;
        }
        if (manualVerificationNote != null) {
            this.manualVerificationNote = manualVerificationNote;
        }
        if (lastManualVerifiedAt != null) {
            this.lastManualVerifiedAt = lastManualVerifiedAt;
        }
        this.collectionPermissionStatus = calculatePermissionStatus(manualPermissionStatus);
    }

    public void updateRobotsCheckStatus(RobotsCheckStatus robotsCheckStatus, String policyCheckNote) {
        this.robotsCheckStatus = robotsCheckStatus == null ? RobotsCheckStatus.UNKNOWN : robotsCheckStatus;
        this.lastPolicyCheckedAt = LocalDateTime.now();
        if (policyCheckNote != null) {
            this.policyCheckNote = policyCheckNote;
        }
        this.collectionPermissionStatus = calculatePermissionStatus(this.collectionPermissionStatus);
    }

    public void updateLoginRequiredPolicy(Boolean loginRequired, String policyCheckNote) {
        this.loginRequired = loginRequired;
        this.lastPolicyCheckedAt = LocalDateTime.now();
        if (policyCheckNote != null) {
            this.policyCheckNote = policyCheckNote;
        }
        this.collectionPermissionStatus = calculatePermissionStatus(this.collectionPermissionStatus);
    }

    public void updateTermsCheck(
            TermsCheckStatus termsCheckStatus,
            String termsUrl,
            LocalDate termsCheckedAt,
            String termsMemo
    ) {
        if (termsCheckStatus != null) {
            this.termsCheckStatus = termsCheckStatus;
        }
        if (termsUrl != null) {
            this.termsUrl = termsUrl;
        }
        if (termsCheckedAt != null) {
            this.termsCheckedAt = termsCheckedAt;
        }
        if (termsMemo != null) {
            this.termsMemo = termsMemo;
            this.policyCheckNote = termsMemo;
        }
        this.lastPolicyCheckedAt = LocalDateTime.now();
        this.collectionPermissionStatus = calculatePermissionStatus(this.collectionPermissionStatus);
    }

    public void updateTermsLinkCandidates(List<TermsLinkCandidate> candidates) {
        this.termsLinkCandidatesJson = TermsLinkCandidateJson.write(candidates);
    }

    public void changeActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void markSuccess(String contentHash) {
        this.lastFetchedAt = LocalDateTime.now();
        this.lastContentHash = contentHash;
        this.lastStatus = SourceWatchStatus.SUCCESS;
        this.failureCount = 0;
    }

    public void markSkipped() {
        this.lastFetchedAt = LocalDateTime.now();
        this.lastStatus = SourceWatchStatus.SKIPPED;
    }

    public void markFailed() {
        this.lastFetchedAt = LocalDateTime.now();
        this.lastStatus = SourceWatchStatus.FAILED;
        this.failureCount = this.failureCount + 1;
    }

    public void updateNextFetchAt(LocalDateTime nextFetchAt) {
        this.nextFetchAt = nextFetchAt;
    }

    public CollectionPermissionStatus calculatePermissionStatus(CollectionPermissionStatus manualPermissionStatus) {
        if (manualPermissionStatus == CollectionPermissionStatus.MANUAL_REVIEW_ONLY) {
            return CollectionPermissionStatus.MANUAL_REVIEW_ONLY;
        }
        if (robotsCheckStatus == RobotsCheckStatus.DISALLOWED) {
            return CollectionPermissionStatus.BLOCKED_BY_ROBOTS;
        }
        if (robotsCheckStatus == RobotsCheckStatus.FETCH_FAILED || robotsCheckStatus == RobotsCheckStatus.PARSE_FAILED) {
            return CollectionPermissionStatus.UNKNOWN_NEEDS_REVIEW;
        }
        if (termsCheckStatus == TermsCheckStatus.RESTRICTION_FOUND || termsCheckStatus == TermsCheckStatus.BLOCKED) {
            return CollectionPermissionStatus.BLOCKED_BY_TERMS;
        }
        if (termsCheckStatus == TermsCheckStatus.NEEDS_REVIEW) {
            return CollectionPermissionStatus.UNKNOWN_NEEDS_REVIEW;
        }
        if (Boolean.TRUE.equals(loginRequired)) {
            return CollectionPermissionStatus.LOGIN_REQUIRED;
        }
        if ((robotsCheckStatus == RobotsCheckStatus.ALLOWED || robotsCheckStatus == RobotsCheckStatus.NOT_FOUND)
                && (termsCheckStatus == TermsCheckStatus.NO_RESTRICTION_FOUND || termsCheckStatus == TermsCheckStatus.NOT_CHECKED)) {
            return CollectionPermissionStatus.ALLOWED_TO_COLLECT;
        }
        return CollectionPermissionStatus.UNKNOWN_NEEDS_REVIEW;
    }

    public Long getId() {
        return id;
    }

    public Brand getBrand() {
        return brand;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getLastFetchedAt() {
        return lastFetchedAt;
    }

    public String getLastContentHash() {
        return lastContentHash;
    }

    public SourceWatchStatus getLastStatus() {
        return lastStatus;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public LocalDateTime getNextFetchAt() {
        return nextFetchAt;
    }

    public Boolean getLoginRequired() {
        return loginRequired;
    }

    public CollectionPermissionStatus getCollectionPermissionStatus() {
        return collectionPermissionStatus;
    }

    public RobotsCheckStatus getRobotsCheckStatus() {
        return robotsCheckStatus;
    }

    public TermsCheckStatus getTermsCheckStatus() {
        return termsCheckStatus;
    }

    public CollectionMethod getCollectionMethod() {
        return collectionMethod;
    }

    public LocalDateTime getLastPolicyCheckedAt() {
        return lastPolicyCheckedAt;
    }

    public LocalDateTime getLastManualVerifiedAt() {
        return lastManualVerifiedAt;
    }

    public String getPolicyCheckNote() {
        return policyCheckNote;
    }

    public String getManualVerificationNote() {
        return manualVerificationNote;
    }

    public String getTermsUrl() {
        return termsUrl;
    }

    public LocalDate getTermsCheckedAt() {
        return termsCheckedAt;
    }

    public String getTermsMemo() {
        return termsMemo;
    }

    public String getTermsLinkCandidatesJson() {
        return termsLinkCandidatesJson;
    }
}
