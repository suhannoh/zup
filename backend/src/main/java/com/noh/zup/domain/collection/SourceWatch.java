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
import java.time.LocalDateTime;

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

    private LocalDateTime lastFetchedAt;

    @Column(length = 64)
    private String lastContentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceWatchStatus lastStatus = SourceWatchStatus.READY;

    @Column(nullable = false)
    private Integer failureCount = 0;

    private LocalDateTime nextFetchAt;

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
}
