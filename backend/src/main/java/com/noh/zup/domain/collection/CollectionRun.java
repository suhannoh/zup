package com.noh.zup.domain.collection;

import com.noh.zup.common.entity.BaseTimeEntity;
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
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_runs")
public class CollectionRun extends BaseTimeEntity {

    private static final int MESSAGE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_watch_id", nullable = false)
    private SourceWatch sourceWatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionRunStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long durationMillis;

    @Column(nullable = false)
    private Boolean fetched = false;

    @Column(nullable = false)
    private Boolean sameAsPrevious = false;

    @Column(nullable = false)
    private Integer candidateCount = 0;

    private Long snapshotId;

    @Column(length = 100)
    private String failureReason;

    @Column(length = MESSAGE_MAX_LENGTH)
    private String errorMessage;

    protected CollectionRun() {
    }

    public CollectionRun(SourceWatch sourceWatch, CollectionTriggerType triggerType) {
        this.sourceWatch = sourceWatch;
        this.triggerType = triggerType;
        this.status = CollectionRunStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void completeSuccess(Boolean fetched, Boolean sameAsPrevious, Integer candidateCount, Long snapshotId) {
        this.status = CollectionRunStatus.SUCCESS;
        this.fetched = fetched;
        this.sameAsPrevious = sameAsPrevious;
        this.candidateCount = candidateCount;
        this.snapshotId = snapshotId;
        this.failureReason = null;
        this.errorMessage = null;
        complete();
    }

    public void completeSkipped(String failureReason, String errorMessage) {
        this.status = CollectionRunStatus.SKIPPED;
        this.fetched = false;
        this.sameAsPrevious = false;
        this.candidateCount = 0;
        this.snapshotId = null;
        this.failureReason = failureReason;
        this.errorMessage = truncate(errorMessage);
        complete();
    }

    public void completeFailed(Boolean fetched, String failureReason, String errorMessage) {
        this.status = CollectionRunStatus.FAILED;
        this.fetched = fetched;
        this.sameAsPrevious = false;
        this.candidateCount = 0;
        this.snapshotId = null;
        this.failureReason = failureReason;
        this.errorMessage = truncate(errorMessage);
        complete();
    }

    private void complete() {
        this.endedAt = LocalDateTime.now();
        this.durationMillis = Duration.between(startedAt, endedAt).toMillis();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MESSAGE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, MESSAGE_MAX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public SourceWatch getSourceWatch() {
        return sourceWatch;
    }

    public CollectionTriggerType getTriggerType() {
        return triggerType;
    }

    public CollectionRunStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public Long getDurationMillis() {
        return durationMillis;
    }

    public Boolean getFetched() {
        return fetched;
    }

    public Boolean getSameAsPrevious() {
        return sameAsPrevious;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
