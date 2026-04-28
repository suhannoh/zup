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

    @Column(nullable = false)
    private Boolean sameAsPrevious;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    protected PageSnapshot() {
    }

    public PageSnapshot(SourceWatch sourceWatch, String contentHash, String extractedText, Boolean sameAsPrevious) {
        this.sourceWatch = sourceWatch;
        this.contentHash = contentHash;
        this.extractedText = extractedText;
        this.sameAsPrevious = sameAsPrevious;
        this.fetchedAt = LocalDateTime.now();
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

    public Boolean getSameAsPrevious() {
        return sameAsPrevious;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }
}
