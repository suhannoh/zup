package com.noh.zup.domain.source;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.collection.CollectionMethod;
import java.time.LocalDate;

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

@Entity
@Table(name = "benefit_sources")
public class BenefitSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SourceType sourceType;

    @Column(nullable = false, length = 1000)
    private String sourceUrl;

    @Column(length = 1000)
    private String officialSourceUrl;

    @Column(length = 300)
    private String sourceTitle;

    private LocalDate sourceCheckedAt;

    private LocalDate lastVerifiedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionMethod collectionMethod = CollectionMethod.UNKNOWN;

    @Column(columnDefinition = "text")
    private String verificationSummary;

    @Column(columnDefinition = "text")
    private String sourceNotice;

    @Column(length = 128)
    private String contentHash;

    @Column(columnDefinition = "text")
    private String memo;

    @Column(nullable = false)
    private Boolean isActive = true;

    protected BenefitSource() {
    }

    public BenefitSource(Benefit benefit, SourceType sourceType, String sourceUrl) {
        this.benefit = benefit;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.officialSourceUrl = sourceUrl;
    }

    public void update(
            SourceType sourceType,
            String sourceUrl,
            String sourceTitle,
            LocalDate sourceCheckedAt,
            String memo
    ) {
        if (sourceType != null) {
            this.sourceType = sourceType;
        }
        if (sourceUrl != null) {
            this.sourceUrl = sourceUrl;
        }
        if (sourceTitle != null) {
            this.sourceTitle = sourceTitle;
        }
        if (sourceCheckedAt != null) {
            this.sourceCheckedAt = sourceCheckedAt;
            this.lastVerifiedDate = sourceCheckedAt;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }

    public void updateVerificationMetadata(
            String officialSourceUrl,
            LocalDate lastVerifiedDate,
            CollectionMethod collectionMethod,
            String verificationSummary,
            String sourceNotice
    ) {
        if (officialSourceUrl != null) {
            this.officialSourceUrl = officialSourceUrl;
            this.sourceUrl = officialSourceUrl;
        }
        if (lastVerifiedDate != null) {
            this.lastVerifiedDate = lastVerifiedDate;
            this.sourceCheckedAt = lastVerifiedDate;
        }
        if (collectionMethod != null) {
            this.collectionMethod = collectionMethod;
        }
        if (verificationSummary != null) {
            this.verificationSummary = verificationSummary;
        }
        if (sourceNotice != null) {
            this.sourceNotice = sourceNotice;
        }
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Long getId() {
        return id;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getOfficialSourceUrl() {
        return officialSourceUrl;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public LocalDate getSourceCheckedAt() {
        return sourceCheckedAt;
    }

    public LocalDate getLastVerifiedDate() {
        return lastVerifiedDate;
    }

    public CollectionMethod getCollectionMethod() {
        return collectionMethod;
    }

    public String getVerificationSummary() {
        return verificationSummary;
    }

    public String getSourceNotice() {
        return sourceNotice;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getMemo() {
        return memo;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
