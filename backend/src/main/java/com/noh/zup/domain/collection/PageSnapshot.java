package com.noh.zup.domain.collection;

import com.noh.zup.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "page_snapshots")
public class PageSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_watch_id", nullable = false)
    private SourceWatch sourceWatch;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Lob
    @Column(nullable = false)
    private String extractedText;

    @Lob
    private String benefitDetailImageSources;

    @Column(nullable = false)
    private Boolean sameAsPrevious;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false)
    private Boolean isForReviewOnly = false;

    private LocalDateTime expiresAt;

    protected PageSnapshot() {
    }

    public PageSnapshot(SourceWatch sourceWatch, String contentHash, String extractedText, Boolean sameAsPrevious) {
        this(sourceWatch, contentHash, extractedText, null, sameAsPrevious);
    }

    public PageSnapshot(
            SourceWatch sourceWatch,
            String contentHash,
            String extractedText,
            String benefitDetailImageSources,
            Boolean sameAsPrevious
    ) {
        this.sourceWatch = sourceWatch;
        this.contentHash = contentHash;
        this.extractedText = extractedText;
        this.benefitDetailImageSources = benefitDetailImageSources;
        this.sameAsPrevious = sameAsPrevious;
        this.fetchedAt = LocalDateTime.now();
        if (sourceWatch != null && sourceWatch.getCollectionPermissionStatus() != CollectionPermissionStatus.ALLOWED_TO_COLLECT) {
            this.isForReviewOnly = true;
            this.expiresAt = this.fetchedAt.plusDays(30);
        }
    }

    public Long getId() {
        return id;
    }

    public SourceWatch getSourceWatch() {
        return sourceWatch;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getBenefitDetailImageSources() {
        return benefitDetailImageSources;
    }

    public Boolean getSameAsPrevious() {
        return sameAsPrevious;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public Boolean getIsForReviewOnly() {
        return isForReviewOnly;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
