package com.noh.zup.domain.source;

import com.noh.zup.common.entity.BaseTimeEntity;
import com.noh.zup.domain.benefit.Benefit;
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

    @Column(length = 300)
    private String sourceTitle;

    private LocalDate sourceCheckedAt;

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
        }
        if (memo != null) {
            this.memo = memo;
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

    public String getSourceTitle() {
        return sourceTitle;
    }

    public LocalDate getSourceCheckedAt() {
        return sourceCheckedAt;
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
